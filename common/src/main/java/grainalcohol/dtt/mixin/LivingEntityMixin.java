package grainalcohol.dtt.mixin;

import dev.architectury.event.EventResult;
import grainalcohol.dtt.api.event.SymptomEvent;
import grainalcohol.dtt.api.internal.AnorexiaController;
import grainalcohol.dtt.init.DTTHintMessage;
import grainalcohol.dtt.init.DTTStatusEffect;
import grainalcohol.dtt.api.wrapper.MentalHealthStatus;
import grainalcohol.dtt.api.wrapper.MentalIllnessStatus;
import grainalcohol.dtt.api.wrapper.Severity;
import grainalcohol.dtt.util.MathUtil;
import net.depression.mental.MentalStatus;
import net.depression.server.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements AnorexiaController {
    @Shadow protected int useItemRemaining;
    @Unique private boolean dtt$shouldInterruptEating = false;

    @Override
    public void dtt$setShouldInterruptEating(boolean shouldInterrupt) {
        this.dtt$shouldInterruptEating = shouldInterrupt;
    }

    @Inject(
            method = "updateUsingItem",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/LivingEntity;useItemRemaining:I",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void interruptEating(ItemStack stack, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(stack.has(DataComponents.FOOD) && self instanceof ServerPlayer player)) {
            return;
        }

        int maxUseTime = stack.getUseDuration(self);
        if (maxUseTime <= 0) return;

        // 进食进度超过50%
        double eatingProgress = (double) (maxUseTime - this.useItemRemaining) / maxUseTime;
        if (eatingProgress <= 0.5) return;

        MentalStatus mentalStatus = Registry.mentalStatus.get(player.getUUID());
        if (this.dtt$shouldInterruptEating) {
            EventResult eventResult = SymptomEvent.INTERRUPT_EATING.invoker().onInterruptEating(player, MentalHealthStatus.from(mentalStatus));

            if (eventResult.isPresent() && eventResult.isFalse()) {
                // false
                this.dtt$shouldInterruptEating = false;
                return;
            }

            // true & default
            // 没有厌食效果，且饥饿值>=4。打断
            player.releaseUsingItem();
            Severity severity = MentalIllnessStatus.from(mentalStatus).getSeverity();
            int durationTicks = MathUtil.inRange(new Random(), 20 * 10, 20 * 20) * Math.max(1, severity.getLevel()); // 10~20秒 * 严重度（1~3）
            if (severity.isSeverelyIll()) {
                durationTicks += MathUtil.inRange(new Random(), 20 * 20, 20 * 60); // 20~60秒
            }
            player.addEffect(new MobEffectInstance(
                    net.depression.effect.EffectHolders.resolve(DTTStatusEffect.ANOREXIA), durationTicks, Math.max(0, severity.getLevel() - 1),
                    false, true, true)
            );
            DTTHintMessage.ANOREXIA_MESSAGE.trigger(player);
            this.dtt$shouldInterruptEating = false;
        }
    }
}
