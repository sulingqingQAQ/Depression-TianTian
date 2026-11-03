package grainalcohol.dtt.mixin.modification;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import grainalcohol.dtt.config.DTTConfig;
import grainalcohol.dtt.util.StringUtil;
import net.depression.client.ClientMentalIllness;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Random;

@Mixin(value = ClientMentalIllness.class, remap = false)
public class ClientMentalIllnessMixin {
    @WrapOperation(
            method = "receiveCloseEyePacket",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
                    remap = true
            )
    )
    private MutableComponent closeEyesMessageVariant(String originalKey, Operation<MutableComponent> original) {
        // TODO: 使文案能够根据精神健康状态不同而不同
        int variantCount = DTTConfig.getInstance().getClientConfig().messageVariantConfig.closeEyesMessageVariantCount;
        if (variantCount > 0) {
            return original.call(StringUtil.findTranslationKeyVariant(
                    originalKey, variantCount, new Random()
            ));
        }
        return original.call(originalKey);
    }
}
