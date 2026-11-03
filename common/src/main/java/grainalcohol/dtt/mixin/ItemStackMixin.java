package grainalcohol.dtt.mixin;

import dev.architectury.event.EventResult;
import grainalcohol.dtt.api.event.SymptomEvent;
import grainalcohol.dtt.api.internal.AnorexiaController;
import grainalcohol.dtt.init.DTTHintMessage;
import grainalcohol.dtt.init.DTTStatusEffect;
import grainalcohol.dtt.api.wrapper.MentalHealthStatus;
import grainalcohol.dtt.api.helper.MentalStatusHelper;
import net.depression.mental.MentalStatus;
import net.depression.server.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUseHead(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        FoodProperties foodProperties = stack.get(DataComponents.FOOD);
        if (foodProperties == null) return;

        if (!(user instanceof ServerPlayer player)) return;

        if (player.hasEffect(DTTStatusEffect.ANOREXIA)) {
            // 厌食且没有特别饥饿时，禁止进食
            if (player.getFoodData().getFoodLevel() >= 4) {
                cir.setReturnValue(InteractionResultHolder.fail(stack));
            }

            // 特别饥饿时允许进食，但延长厌食症状态时间
            MobEffectInstance anorexiaStatusEffect = player.getEffect(DTTStatusEffect.ANOREXIA);
            if (anorexiaStatusEffect == null) {
                return;
            }

            player.removeEffect(DTTStatusEffect.ANOREXIA);
            // 1.21 的 saturation() 是最终饱和度 = nutrition * 修饰值 * 2，这里还原回旧版的饱和度修饰值
            float saturationModifier = foodProperties.nutrition() > 0 ? foodProperties.saturation() / (2.0F * foodProperties.nutrition()) : 0.0F;
            int extraDuration = (foodProperties.nutrition() + (int) saturationModifier) * 20 * 2; // 每点饥饿值和饱和度各增加1秒
            player.addEffect(new MobEffectInstance(
                    DTTStatusEffect.ANOREXIA,
                    Math.max(0, anorexiaStatusEffect.getDuration() + extraDuration),
                    anorexiaStatusEffect.getAmplifier(),
                    anorexiaStatusEffect.isAmbient(),
                    anorexiaStatusEffect.isVisible(),
                    anorexiaStatusEffect.showIcon()
            ));
            player.addEffect(new MobEffectInstance(
                    MobEffects.CONFUSION, 6 * 20, 1
            ));
            DTTHintMessage.ANOREXIA_WORSEN_MESSAGE.trigger(player);
        } else {
            // 没有厌食时，随机触发厌食症
            MentalStatus mentalStatus = Registry.mentalStatus.get(player.getUUID());
            boolean shouldTriggerAnorexia = MentalStatusHelper.shouldTriggerAnorexia(mentalStatus);

            // 且饥饿值大于4
            if (player.getFoodData().getFoodLevel() < 4) {
                shouldTriggerAnorexia = false;
            }

            if (shouldTriggerAnorexia) {
                EventResult eventResult = SymptomEvent.ANOREXIA_TRIGGERED.invoker().onAnorexiaTriggered(player, MentalHealthStatus.from(mentalStatus));

                if (eventResult.isPresent() && eventResult.isFalse()) {
                    // false
                    shouldTriggerAnorexia = false;
                }
            }

            // true & default
            // 设置打断进食标志
            if (player instanceof AnorexiaController controller) {
                controller.dtt$setShouldInterruptEating(shouldTriggerAnorexia);
            }
        }
    }
}
