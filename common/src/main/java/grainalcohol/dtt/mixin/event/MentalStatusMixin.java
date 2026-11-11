package grainalcohol.dtt.mixin.event;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import grainalcohol.dtt.api.event.EmotionEvent;
import grainalcohol.dtt.api.event.MentalIllnessEvent;
import grainalcohol.dtt.api.event.PTSDContext;
import grainalcohol.dtt.api.event.PTSDEvent;
import grainalcohol.dtt.api.wrapper.MentalIllnessStatus;
import grainalcohol.dtt.api.helper.PTSDHelper;
import grainalcohol.dtt.api.wrapper.PTSDLevel;
import net.depression.mental.MentalStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(value = MentalStatus.class, remap = false)
public class MentalStatusMixin {
    @Shadow private ServerPlayer player;

    // 上一有效病情。NONE 表示"没有可比较的有效病情"（尚未建立基线、存档里是 none 或未知串、疾病 ID 未识别），
    // 它不等于 HEALTHY，也不参与恶化/好转比较。初值必须是 NONE：默认 HEALTHY 会把首次观察到的
    // mdd_patient / bd_patient 等已有疾病误记成 HEALTHY→严重疾病（假 WORSENED）。
    // 同时它必须保持非 null，初始化前 writeNbt 才能取到名称。
    @Unique private MentalIllnessStatus dtt$lastTickMentalIllnessStatus = MentalIllnessStatus.NONE;
    @Unique private boolean dtt$lastTickIsInCombatState = false;
    @Unique private boolean dtt$lastTickIsManicPhase = false;

    // 注意：MentalStatus 的 lambda 序号由"整个类"共用的计数器生成，主包任何位置新增 lambda 都会整体位移。
    // 0b48eca（t28 帧率修复）在静态初始化里新增了 lambda$static$0，其后序号统一 +1：
    // tick$1=实体分支、tick$2=非实体分支、removePTSD$5=实体分支、removePTSD$6=非实体分支。
    @Inject(
            method = "lambda$tick$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/depression/network/ActionbarHintPacket;sendPTSDFormPacket(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/Component;)V",
                    remap = true
            )
    )
    private static void PTSDFormEvent1(ServerPlayer player, EntityType<?> entityType, CallbackInfo ci) {
        String ptsdId = EntityType.getKey(entityType).toString();
        PTSDContext context = PTSDContext.of(ptsdId, entityType.getDescription().getString());
        PTSDEvent.PTSD_FORM_EVENT.invoker().onPTSDFormed(player, context, PTSDLevel.LATENT);
    }

    @WrapOperation(
            method = "lambda$tick$2",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/depression/network/ActionbarHintPacket;sendPTSDFormPacket(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/Component;)V",
                    remap = true
            )
    )
    private void PTSDFormEvent2(ServerPlayer player, Component id, Operation<Void> original, @Local(argsOnly = true, ordinal = 0) String key) {
        PTSDContext context = PTSDContext.of(key, id.getString());
        PTSDEvent.PTSD_FORM_EVENT.invoker().onPTSDFormed(player, context, PTSDLevel.LATENT);
        original.call(player, id);
    }

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/concurrent/ConcurrentHashMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private Object PTSDLevelChangedEvent(
            ConcurrentHashMap<String, Double> map, Object key, Object value,
            Operation<Object> original,
            @Local(name = "originValue") Double originValue
    ) {
        PTSDLevel lastLevel = PTSDHelper.getPTSDLevel(originValue);
        PTSDLevel currentLevel = PTSDHelper.getPTSDLevel((Double) value);
        if (lastLevel != currentLevel) {
            // PTSD等级发生变化
            AtomicReference<String> info = new AtomicReference<>();

            EntityType.byString((String) key).ifPresentOrElse(entityType -> {
                info.set(entityType.getDescription().getString());
            }, () -> info.set((String) key));

            PTSDContext context = PTSDContext.of((String) key, info.get());
            PTSDEvent.PTSD_LEVEL_CHANGED_EVENT.invoker().onPTSDLevelChanged(this.player, context, lastLevel, currentLevel);
        }
        return original.call(map, key, value);
    }

    @Inject(
            method = "lambda$removePTSD$5",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/depression/network/ActionbarHintPacket;sendPTSDDispersePacket(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/Component;)V",
                    remap = true
            )
    )
    private void PTSDDisperseEvent1(EntityType<?> entityType, CallbackInfo ci) {
        String ptsdId = EntityType.getKey(entityType).toString();
        PTSDContext context = PTSDContext.of(ptsdId, entityType.getDescription().getString());
        PTSDEvent.PTSD_DISPERSE_EVENT.invoker().onPTSDDisperse(this.player, context);
    }

    @WrapOperation(
            method = "lambda$removePTSD$6",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/depression/network/ActionbarHintPacket;sendPTSDDispersePacket(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/Component;)V",
                    remap = true
            )
    )
    private void PTSDDisperseEvent2(ServerPlayer player, Component id, Operation<Void> original, @Local(argsOnly = true, ordinal = 0) String key) {
        PTSDContext context = PTSDContext.of(key, id.getString());
        PTSDEvent.PTSD_DISPERSE_EVENT.invoker().onPTSDDisperse(player, context);
        original.call(player, id);
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/depression/network/MentalStatusPacket;sendToPlayer(Lnet/minecraft/server/level/ServerPlayer;Lnet/depression/mental/MentalStatus;)V",
                    shift = At.Shift.AFTER,
                    remap = true
            )
    )
    private void mentalIllnessChangedEvent(ServerPlayer player, CallbackInfo ci) {
        MentalStatus self = (MentalStatus) (Object) this;
        MentalIllnessStatus currentIllness = MentalIllnessStatus.from(self);

        if (dtt$lastTickMentalIllnessStatus == null) {
            // 防御：正常状态机不会让它为 null，兜底同样只建立基线（current 也可以是 NONE）
            dtt$lastTickMentalIllnessStatus = currentIllness;
            return;
        }

        if (currentIllness == MentalIllnessStatus.NONE) {
            // 当前没有可比较的有效病情（疾病 ID 未识别 / 取不到 MentalStatus 等）：
            // 本次不发事件，也不覆盖上一有效病情，避免下游拿 NONE 做恶化/好转比较
            return;
        }

        if (dtt$lastTickMentalIllnessStatus == MentalIllnessStatus.NONE) {
            // 上一状态是 NONE（首次观察 / 存档是 none 或未知串）：只建立基线，本次不发事件
            dtt$lastTickMentalIllnessStatus = currentIllness;
            return;
        }

        if (dtt$lastTickMentalIllnessStatus == currentIllness) {
            // 患病情况未改变
            return;
        }

        // 患病情况改变
        MentalIllnessEvent.MENTAL_HEALTH_CHANGED_EVENT.invoker().onMentalIllnessChanged(player, dtt$lastTickMentalIllnessStatus, currentIllness);

        dtt$lastTickMentalIllnessStatus = currentIllness;
    }

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/depression/network/MentalStatusPacket;sendToPlayer(Lnet/minecraft/server/level/ServerPlayer;Lnet/depression/mental/MentalStatus;)V",
                    shift = At.Shift.AFTER,
                    remap = true
            )
    )
    private void combatStateChangedEvent(CallbackInfo ci) {
        MentalStatus self = (MentalStatus) (Object) this;
        boolean currentIsInCombatState = (self.combatCountdown > 0);

        if (dtt$lastTickIsInCombatState == currentIsInCombatState) {
            // 战斗状态未改变
            return;
        }

        if (!dtt$lastTickIsInCombatState && currentIsInCombatState) {
            // 进入战斗状态
            EmotionEvent.ENTER_COMBAT_STATE_EVENT.invoker().onEnterCombatState(player);
        }

        if (dtt$lastTickIsInCombatState && !currentIsInCombatState) {
            // 退出战斗状态
            EmotionEvent.EXIT_COMBAT_STATE_EVENT.invoker().onExitCombatState(player);
        }

        dtt$lastTickIsInCombatState = currentIsInCombatState;
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void readNbt(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("lastTickMentalIllnessStatus")) {
            dtt$lastTickMentalIllnessStatus = MentalIllnessStatus.from(nbt.getString("lastTickMentalIllnessStatus"));
        }
        if (nbt.contains("lastTickIsInCombatState")) {
            dtt$lastTickIsInCombatState = nbt.getBoolean("lastTickIsInCombatState");
        }
        if (nbt.contains("lastTickIsManicPhase")) {
            dtt$lastTickIsManicPhase = nbt.getBoolean("lastTickIsManicPhase");
        }
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void writeNbt(CompoundTag nbt, CallbackInfo ci) {
        nbt.putString("lastTickMentalIllnessStatus", dtt$lastTickMentalIllnessStatus.getName());
        nbt.putBoolean("lastTickIsInCombatState", dtt$lastTickIsInCombatState);
        nbt.putBoolean("lastTickIsManicPhase", dtt$lastTickIsManicPhase);
    }
}
