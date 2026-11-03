package grainalcohol.dtt.hint.messages;

import grainalcohol.dtt.api.helper.EmotionHelper;
import grainalcohol.dtt.hint.SubscriptionHintMessage;
import grainalcohol.dtt.hint.timer.TimeUnit;
import grainalcohol.dtt.hint.timer.Timer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ResetSpawnPointMessage extends SubscriptionHintMessage {
    public ResetSpawnPointMessage(ResourceLocation identifier) {
        super(identifier, true, 3);
    }

    @Override
    public @NotNull String getTranslationKey() {
        return "hint.dtt.reset_spawn_point";
    }

    @Override
    public @Nullable Consumer<ServerPlayer> getAfterSend() {
        return player -> EmotionHelper.mentalHeal(player, getIdentifier(), 2.0);
    }

    @Override
    public @NotNull Timer getCooldownTimer() {
        return DEFAULT_TIMER;
    }
}
