package grainalcohol.dtt.hint.messages;

import grainalcohol.dtt.hint.timer.Timer;
import grainalcohol.dtt.hint.SubscriptionHintMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class LatentPTSDMessage extends SubscriptionHintMessage {
    public LatentPTSDMessage(ResourceLocation identifier) {
        super(identifier, false, 3);
    }

    @Override
    public @NotNull String getTranslationKey() {
        return "hint.dtt.ptsd_latent";
    }

    @Override
    public @Nullable Consumer<ServerPlayer> getAfterSend() {
        return null;
    }

    @Override
    public @NotNull Timer getCooldownTimer() {
        return DEFAULT_TIMER;
    }
}
