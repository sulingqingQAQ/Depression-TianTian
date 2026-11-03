package grainalcohol.dtt.mixin.event;

import dev.architectury.event.EventResult;
import grainalcohol.dtt.api.event.EmotionEvent;
import net.depression.server.Registry;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Registry.class, remap = false)
public class DepressionRegistryMixin {
    @Inject(method = "eventAddPlayer", at = @At("HEAD"), cancellable = true)
    private static void onAddPlayerHead(ServerBossEvent event, ServerPlayer player, CallbackInfo ci) {
        EventResult eventResult = EmotionEvent.ZEN_STATE_ADDITION_EVENT.invoker().onZenStateChange(event, player);

        if (eventResult.isPresent() && eventResult.isFalse()) {
            // false
            ci.cancel();
        }
        // true & default
    }

    @Inject(method = "eventRemovePlayer", at = @At("HEAD"), cancellable = true)
    private static void onRemovePlayerHead(ServerBossEvent event, ServerPlayer player, CallbackInfo ci) {
        EventResult eventResult = EmotionEvent.ZEN_STATE_REMOVAL_EVENT.invoker().onZenStateChange(event, player);

        if (eventResult.isPresent() && eventResult.isFalse()) {
            // false
            ci.cancel();
        }
        // true & default
    }

    @Inject(method = "eventAddPlayer", at = @At("TAIL"))
    private static void onEnterZenState(ServerBossEvent event, ServerPlayer player, CallbackInfo ci) {
        if (Registry.playerEventMap.get(player.getUUID()).size() == 1) {
            EmotionEvent.ENTER_ZEN_STATE_EVENT.invoker().onEnterZenState(player);
        }
    }

    @Inject(method = "eventRemovePlayer", at = @At("TAIL"))
    private static void onExitZenState(ServerBossEvent event, ServerPlayer player, CallbackInfo ci) {
        if (Registry.playerEventMap.get(player.getUUID()).isEmpty()) {
            EmotionEvent.EXIT_ZEN_STATE_EVENT.invoker().onExitZenState(player);
        }
    }
}
