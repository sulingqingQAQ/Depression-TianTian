package grainalcohol.dtt.mixin.modification;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import grainalcohol.dtt.DTTMod;
import grainalcohol.dtt.api.internal.BlockBreakMentalHealCooldownController;
import grainalcohol.dtt.config.DTTConfig;
import grainalcohol.dtt.config.ServerConfig;
import grainalcohol.dtt.mock.MockMentalStatus;
import grainalcohol.dtt.util.MiscUtil;
import net.depression.mental.MentalStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = MentalStatus.class, remap = false)
public abstract class MentalStatusMixin implements BlockBreakMentalHealCooldownController {
    @Unique private int dtt$blockBreakHealCooldownTicks = 0;
    @Unique private double dtt$maxNearbyHealAmount = 0;

    @Shadow public abstract void mentalHurt(double value);
    @Shadow @Final public ConcurrentHashMap<String, Double> PTSD;
    @Shadow @Final private ConcurrentHashMap<String, Long> PTSDTimeBuffer;
    @Shadow @Final private ConcurrentHashMap<String, Double> PTSDValueBuffer;
    @Shadow @Final public HashSet<String> playerPTSDSet;
    @Shadow public long tickCount;
    @Shadow private ServerPlayer player;
    @Shadow public abstract double mentalHeal(double value);

    @Shadow
    @Final
    private ConcurrentHashMap<String, Integer> boredom;

    @Unique
    @Override
    public int dtt$getCooldownTicks() {
        return this.dtt$blockBreakHealCooldownTicks;
    }

    @Unique
    @Override
    public void dtt$setCooldownTicks(int ticks) {
        this.dtt$blockBreakHealCooldownTicks = ticks;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        if (this.dtt$blockBreakHealCooldownTicks > 0) {
            this.dtt$blockBreakHealCooldownTicks--;
        }
    }

    // 方块光环扫描的门控：主包把"快照 + 提交"收进了 submitHealBlockScan()，tick 只调它。
    // 这里在方法入口按配置间隔取消 —— 取消发生在主包的 CAS 占位之前，既不会残留 healBlockScanPending，
    // 也不会白白抓一次已加载区块快照。
    @Inject(method = "submitHealBlockScan", at = @At("HEAD"), cancellable = true)
    private void gateHealBlockScan(CallbackInfo ci) {
        int intervalTicks = DTTConfig.getInstance().getServerConfig().mentalHealConfig.nearbyBlockIntervalTicks;
        if (intervalTicks <= 0 || this.tickCount % intervalTicks == 0) {
            return; // 到点，放行
        }
        ci.cancel();
    }

    @WrapOperation(
            method = "detectNearbyHealBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/depression/mental/MentalStatus;mentalHeal(Ljava/lang/String;D)D"
            )
    )
    private double redirectDetectNearbyHealBlockMentalHeal(MentalStatus mentalStatus, String id, double value, Operation<Double> original) {
        ServerConfig.NearbyMultipleBlocksHealMode nearbyMultipleBlocksHealMode = DTTConfig.getInstance().getServerConfig().mentalHealConfig.nearbyBlockMode;
        if (nearbyMultipleBlocksHealMode.equals(ServerConfig.NearbyMultipleBlocksHealMode.EVERYONE)) {
            // everyone模式：应用所有方块
            return original.call(mentalStatus, id, value);
        }

        if (value > dtt$maxNearbyHealAmount) {
            // max_only模式：仅应用回复量最大的那一个
            dtt$maxNearbyHealAmount = value;
        }
        MockMentalStatus mockMentalStatus = MockMentalStatus.copyOf(mentalStatus);
        return mockMentalStatus.mockMentalHeal(id, value);
    }

    @Inject(
            method = "detectNearbyHealBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/depression/network/ActionbarHintPacket;sendNearbyBlockHealPacket(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/Component;)V",
                    shift = At.Shift.AFTER,
                    remap = true
            )
    )
    private void onDetectNearbyHealBlockSendPacket(CallbackInfo ci) {
        ServerConfig.NearbyMultipleBlocksHealMode nearbyMultipleBlocksHealMode = DTTConfig.getInstance().getServerConfig().mentalHealConfig.nearbyBlockMode;
        if (nearbyMultipleBlocksHealMode.equals(ServerConfig.NearbyMultipleBlocksHealMode.MAX_ONLY)) {
            this.mentalHeal(dtt$maxNearbyHealAmount);
        }
        dtt$maxNearbyHealAmount = 0;
        // nothing模式：什么都不做
    }

    @Inject(method = "viewDetect", at = @At("HEAD"), cancellable = true)
    private void onViewDetectHead(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        double distance = this.player.position().distanceTo(entity.position());
        if (distance > DTTConfig.getInstance().getServerConfig().PTSDConfig.maxDistanceToTriggerPTSDBySight) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(
            method = "mentalHeal(Ljava/lang/String;D)D",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/depression/mental/MentalStatus;mentalHeal(D)D"
            )
    )
    private double wrapMentalHeal(
            MentalStatus instance, double value, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) String string
    ) {
        int noveltyThreshold = DTTConfig.getInstance().getServerConfig().commonConfig.noveltyThreshold;
        if (noveltyThreshold == 0) return original.call(instance, value);

        int boredom = this.boredom.get(string);
        // boredom的初始值是2，并且不会是1
        if (boredom <= noveltyThreshold + 1) {
            return original.call(instance, value * 1.45);
        } else {
            return original.call(instance, value);
        }
    }

    @ModifyConstant(
            method = "mentalHeal(Ljava/lang/String;D)D",
            constant = @Constant(doubleValue = 2.0)
    )
    private double modifyMentalHealBoredomInfluence(double original) {
        double boredomStrength = DTTConfig.getInstance().getServerConfig().commonConfig.boredomStrength;
        return 1 / boredomStrength; // 原数据为2.0，即(1 / 0.5)
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map$Entry;setValue(Ljava/lang/Object;)Ljava/lang/Object;",
                    ordinal = 2
            )
    )
    private Object modifyPTSDRecoveryLatent(Map.Entry<String, Double> PTSDEntry, Object value, Operation<Object> original) {
        if (value instanceof Double doubleValue) {
            ServerConfig.PTSDConfig PTSDConfig = DTTConfig.getInstance().getServerConfig().PTSDConfig;
            double baseMultiplier = PTSDConfig.PTSDRecoveryMultiplier;
            double multiplier = PTSDConfig.PTSDRecoveryMultiplierLatent;
            return original.call(PTSDEntry, doubleValue * baseMultiplier * multiplier);
        }
        return original.call(PTSDEntry, value);
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map$Entry;setValue(Ljava/lang/Object;)Ljava/lang/Object;",
                    ordinal = 3
            )
    )
    private Object modifyPTSDRecoveryActive(Map.Entry<String, Double> PTSDEntry, Object value, Operation<Object> original) {
        if (value instanceof Double doubleValue) {
            ServerConfig.PTSDConfig PTSDConfig = DTTConfig.getInstance().getServerConfig().PTSDConfig;
            double baseMultiplier = PTSDConfig.PTSDRecoveryMultiplier;
            double multiplier = PTSDConfig.PTSDRecoveryMultiplierActive;
            return original.call(PTSDEntry, doubleValue * baseMultiplier * multiplier);
        }
        return original.call(PTSDEntry, value);
    }

    // TODO: 加一个修改mentalHurt中黑暗环境对伤害量加成的mixin

    @WrapOperation(
            method = "mentalHurt(Ljava/lang/String;D)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/ConcurrentHashMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    ordinal = 1
            )
    )
    private Object modifyPTSDBufferValue(
            ConcurrentHashMap<String, Double> PTSDValueBufferMap, Object key, Object value, Operation<Object> original,
            @Local(name = "originValue") Double originValue,
            @Local(argsOnly = true) double damage
    ) {
        double multiplier = DTTConfig.getInstance().getServerConfig().PTSDConfig.PTSDValueGainMultiplier;
        return original.call(PTSDValueBufferMap, key, originValue + damage * multiplier);
    }

    @WrapOperation(
            method = "mentalHurt(Ljava/lang/String;D)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/ConcurrentHashMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    ordinal = 3
            )
    )
    private Object modifyPTSDTimeBufferValue2(
            ConcurrentHashMap<String, Double> PTSDValueBufferMap, Object key, Object value, Operation<Object> original,
            @Local(argsOnly = true) double damage
    ) {
        double multiplier = DTTConfig.getInstance().getServerConfig().PTSDConfig.PTSDValueGainMultiplier;
        return original.call(PTSDValueBufferMap, key, damage * multiplier);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void clearPTSDData(CallbackInfo ci) {
        Set<String> blacklist = DTTConfig.getInstance().getServerConfig().PTSDConfig.universalPTSDBlackList;
        MiscUtil.cleanMap(PTSD, blacklist::contains);
        MiscUtil.cleanMap(PTSDTimeBuffer, blacklist::contains);
        MiscUtil.cleanMap(PTSDValueBuffer, blacklist::contains);
        MiscUtil.cleanSet(playerPTSDSet, blacklist::contains);
    }

    @Inject(method = "mentalHurt(Ljava/lang/String;D)V", at = @At("HEAD"), cancellable = true)
    private void blockPTSDData(String id, double amount, CallbackInfo ci) {
        if (id.equals("player") || id.equals("minecraft:player")) {
            return;
        }
        if (DTTConfig.getInstance().getServerConfig().PTSDConfig.universalPTSDBlackList.contains(id)) {
            DTTMod.LOGGER.info("Blocked mental health value hurt by damage source but string: {}", id);
            mentalHurt(amount);
            ci.cancel();
        }
    }

    @Inject(method = "triggerHurt", at = @At("HEAD"), cancellable = true)
    private void blockPTSDTrigger(String id, CallbackInfo ci) {
        if (id.equals("player") || id.equals("minecraft:player")) {
            return;
        }
        if (DTTConfig.getInstance().getServerConfig().PTSDConfig.universalPTSDBlackList.contains(id)) {
            DTTMod.LOGGER.info("Blocked triggerHintMessage ptsd from hurt by damage type source but string: {}", id);
            ci.cancel();
        }
    }
}
