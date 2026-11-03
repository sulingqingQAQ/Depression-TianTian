package grainalcohol.dtt.mixin.modification;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import grainalcohol.dtt.config.DTTConfig;
import grainalcohol.dtt.api.helper.MentalStatusHelper;
import grainalcohol.dtt.api.wrapper.MentalHealthStatus;
import grainalcohol.dtt.api.wrapper.MentalIllnessStatus;
import net.depression.client.ClientMentalStatus;
import net.depression.item.MentalHealthScaleItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MentalHealthScaleItem.class)
public class MentalHealthScaleItemMixin {
    @WrapWithCondition(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemCooldowns;addCooldown(Lnet/minecraft/world/item/Item;I)V", remap = true))
    private boolean shouldApplyCooldown(ItemCooldowns instance, Item item, int duration){
        // true表示禁用，所以取反
        return !DTTConfig.getInstance().getServerConfig().itemConfig.disableMentalHealthScaleCooldown;
    }

    @Redirect(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/Item;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
                    ordinal = 1,
                    remap = true
            )
    )
    private InteractionResultHolder<ItemStack> itemDecrement(Item instance, Level world, Player player, InteractionHand interactionHand) {
        ItemStack itemStack = player.getItemInHand(interactionHand);
        if (DTTConfig.getInstance().getServerConfig().itemConfig.disposableMentalHealthScale) {
            if (!player.isCreative()) {
                itemStack.shrink(1);
            }
            return InteractionResultHolder.success(itemStack);
        }

        return instance.use(world, player, interactionHand);
    }

    @WrapWithCondition(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;sendSystemMessage(Lnet/minecraft/network/chat/Component;)V",
                    ordinal = 0,
                    remap = true
            )
    )
    private boolean enhancedFirstMessage(Player player, Component message, @Local(name = "mentalStatus") ClientMentalStatus mentalStatus) {
        if (DTTConfig.getInstance().getServerConfig().itemConfig.enhancedMentalHealthScaleAction) {
            // 心理健康指数
            player.sendSystemMessage(
                    Component.translatable(
                            "message.dtt.mental_health_scale.index",
                            String.format("%.2f", MentalStatusHelper.getMentalHealthRateWithEmotionAffection(mentalStatus.mentalHealthValue, mentalStatus.emotionValue, 0.6) * 10) // 这可能是暂时的？
                    ).withStyle(ChatFormatting.UNDERLINE)
                    // 悬停时显示详细数据
                    .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(
                            "text.dtt.mental_health_scale.index.hover",
                            String.format("%.4f", mentalStatus.mentalHealthValue),
                            String.format("%.2f", mentalStatus.emotionValue))))
                    )
            );
            return false;
        }
        return true;
    }

    @WrapWithCondition(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;sendSystemMessage(Lnet/minecraft/network/chat/Component;)V",
                    ordinal = 1,
                    remap = true
            )
    )
    private boolean enhancedSecondMessage(Player player, Component message, @Local(name = "mentalStatus") ClientMentalStatus mentalStatus) {
        if (DTTConfig.getInstance().getServerConfig().itemConfig.enhancedMentalHealthScaleAction) {
            // 心理评估结果
            player.sendSystemMessage(
                    Component.translatable(
                            "message.dtt.mental_health_scale.assessment",
                            MentalHealthStatus.from(mentalStatus).getAssessmentText()
                    ).withStyle(ChatFormatting.UNDERLINE)
                    // 悬停时显示患病情况
                    .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(
                            "text.dtt.mental_health_scale.assessment.hover", MentalIllnessStatus.from(mentalStatus.mentalHealthId).getDisplayText())))
                    )
            );
            return false;
        }
        return true;
    }
}
