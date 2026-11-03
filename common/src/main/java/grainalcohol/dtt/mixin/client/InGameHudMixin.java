package grainalcohol.dtt.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.depression.client.DepressionClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public class InGameHudMixin {
    // 1.21.1 的饥饿条拆到 renderFood 里，用 blitSprite 画：ordinal 0 空、1 满、2 半
    @WrapOperation(
            method = "renderFood",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V",
                    ordinal = 1
            )
    )
    private void redirectFullHungerValueTextures(GuiGraphics context, ResourceLocation sprite, int x, int y, int width, int height, Operation<Void> original) {
        if (DepressionClient.clientMentalStatus.mentalHealthId < 3) {
            original.call(context, sprite, x, y, width, height);
        }
    }

    @WrapOperation(
            method = "renderFood",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V",
                    ordinal = 2
            )
    )
    private void redirectHalfHungerValueTextures(GuiGraphics context, ResourceLocation sprite, int x, int y, int width, int height, Operation<Void> original) {
        if (DepressionClient.clientMentalStatus.mentalHealthId < 3) {
            original.call(context, sprite, x, y, width, height);
        }
    }
}
