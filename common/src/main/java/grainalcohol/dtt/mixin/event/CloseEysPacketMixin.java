package grainalcohol.dtt.mixin.event;

import dev.architectury.event.EventResult;
import grainalcohol.dtt.api.event.SymptomEvent;
import net.depression.network.CloseEyePacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CloseEyePacket.class, remap = false)
public class CloseEysPacketMixin {
    @Inject(method = "sendToPlayer", at = @At("HEAD"), cancellable = true)
    private static void onEyesClosed(ServerPlayer player, CallbackInfo ci) {
        EventResult eventResult = SymptomEvent.SIMPLE_CLOSE_EYES_EVENT.invoker().onCloseEyes(player);

        if (eventResult.isPresent() && eventResult.isFalse()) {
            // false
            ci.cancel();
        }
        // true or default
    }
}
