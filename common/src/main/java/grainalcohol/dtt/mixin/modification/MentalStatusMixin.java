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
    @Unique private String dtt$maxNearbyHealType = null;
    // 主包只在 tickCount % 20 == 0 时调用 submitHealBlockScan，所以间隔要按"第几次提交"换算
    @Unique private int dtt$healBlockScanSubmissions = 0;

    @Shadow public abstract void mentalHurt(double value);
    @Shadow @Final public ConcurrentHashMap<String, Double> PTSD;
    @Shadow @Final private ConcurrentHashMap<String, Long> PTSDTimeBuffer;
    @Shadow @Final private ConcurrentHashMap<String, Double> PTSDValueBuffer;
    @Shadow @Final public HashSet<String> playerPTSDSet;
    @Shadow public long tickCount;
    @Shadow private ServerPlayer player;
    @Shadow public abstract double mentalHeal(double value);
    @Shadow public abstract double mentalHeal(String string, double value);

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
    //
    // 间隔必须换算成"第几次提交"，不能用 tickCount % intervalTicks：主包只在 tickCount % 20 == 0 提交，
    // 两个条件同时成立的实际间隔是 lcm(20, interval) —— interval=30 会变成 60、interval=7 会变成 140。
    @Inject(method = "submitHealBlockScan", at = @At("HEAD"), cancellable = true)
    private void gateHealBlockScan(CallbackInfo ci) {
        int intervalTicks = DTTConfig.getInstance().getServerConfig().mentalHealConfig.nearbyBlockIntervalTicks;
        // <=0 视为原版节奏（每次提交都放行，即 20 tick）；先转 long，避免 intervalTicks + 19 在 int 上溢出
        long everyNth = intervalTicks <= 0 ? 1L : Math.max(1L, ((long) intervalTicks + 19L) / 20L);
        if (++this.dtt$healBlockScanSubmissions >= everyNth) {
            this.dtt$healBlockScanSubmissions = 0;
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
        if (nearbyMultipleBlocksHealMode.equals(ServerConfig.NearbyMultipleBlocksHealMode.NOTHING)) {
            // nothing模式：不模拟、不累积。返回 0 让主包的 totalHealValue 不达标，
            // 不会再出现"发了附近方块治疗提示但实际没治疗"的误导
            return 0d;
        }

        if (value > dtt$maxNearbyHealAmount) {
            // max_only模式：只记本次扫描里回复量最大的那一个，真实结算交给 afterHealBlockScanApplied
            dtt$maxNearbyHealAmount = value;
            dtt$maxNearbyHealType = id;
        }
        MockMentalStatus mockMentalStatus = MockMentalStatus.copyOf(mentalStatus);
        return mockMentalStatus.mockMentalHeal(id, value);
    }

    // 结算门控：主包在每次扫描的主线程结算里调用一次 afterHealBlockScanApplied()（与提示包阈值解耦）。
    // 原来挂在"提示包调用之后"，导致两件事：
    //  1) totalHealValue 没超过 0.25 的扫描不结算，小额来源（torch 0.05 / candle 0.2 / soul_campfire 0.25）
    //     在 max_only 下完全没有治疗效果；精神健康越低，门槛对应的原始值越高（health=3.05 时需要 >8.19）；
    //  2) 没结算的那几轮会把 dtt$maxNearbyHealAmount 留到下一次扫描，之后某轮过门槛时用旧值结算。
    // 现在每次扫描都只结算"当次扫描选中的最大值"，结算后立刻清零。
    @Inject(method = "afterHealBlockScanApplied", at = @At("TAIL"))
    private void settleHealBlockScan(double totalHealValue, CallbackInfo ci) {
        ServerConfig.NearbyMultipleBlocksHealMode nearbyMultipleBlocksHealMode = DTTConfig.getInstance().getServerConfig().mentalHealConfig.nearbyBlockMode;
        if (nearbyMultipleBlocksHealMode.equals(ServerConfig.NearbyMultipleBlocksHealMode.MAX_ONLY) && dtt$maxNearbyHealType != null) {
            // 走真实 mentalHeal(String,double)：保留 boredom 衰减与 novelty 加成，与 everyone 模式同口径
            this.mentalHeal(dtt$maxNearbyHealType, dtt$maxNearbyHealAmount);
        }
        dtt$maxNearbyHealAmount = 0;
        dtt$maxNearbyHealType = null;
        // nothing模式：什么都不做（重定向已返回 0，这里也不会有暂存值）
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
            return original.call(PTSDEntry, dtt$scalePTSDRecovery(PTSDEntry.getValue(), doubleValue, baseMultiplier * multiplier));
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
            return original.call(PTSDEntry, dtt$scalePTSDRecovery(PTSDEntry.getValue(), doubleValue, baseMultiplier * multiplier));
        }
        return original.call(PTSDEntry, value);
    }

    // 主包传进 setValue 的是已经恢复过一次的新值；倍率只能作用在"这一 tick 恢复了多少"上，
    // 不能乘到残余值上（否则默认 1.5 倍时潜伏值会越恢复越高）。此时 setValue 还没执行，getValue() 仍是旧值。
    @Unique
    private static double dtt$scalePTSDRecovery(Double oldValue, double newValue, double factor) {
        if (oldValue == null || factor == 1.0) {
            return newValue; // 倍率 1 直接放行，保证与主包逐值一致
        }
        double recovery = oldValue - newValue;
        return oldValue - recovery * factor;
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
