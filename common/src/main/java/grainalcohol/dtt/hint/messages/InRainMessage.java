package grainalcohol.dtt.hint.messages;

import grainalcohol.dtt.api.helper.EmotionHelper;
import grainalcohol.dtt.hint.SimpleHintMessage;
import grainalcohol.dtt.hint.timer.TimeUnit;
import grainalcohol.dtt.hint.timer.Timer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class InRainMessage extends SimpleHintMessage {
    public InRainMessage(ResourceLocation identifier) {
        super(identifier, false, 3);
    }

    @Override
    public @Nullable Predicate<ServerPlayer> getGlobalCondition() {
        return player -> player.serverLevel().isRainingAt(player.blockPosition());
    }

    @Override
    public @NotNull String getTranslationKey() {
        return "hint.dtt.in_rain";
    }

    @Override
    public @Nullable Consumer<ServerPlayer> getAfterSend() {
        return player -> EmotionHelper.mentalHurt(player, 2.0);
    }

    @Override
    public @NotNull Timer getCooldownTimer() {
        return DEFAULT_TIMER;
    }

    @Override
    public @Nullable Timer getAccumulateTimer() {
        return null;
    }
}
