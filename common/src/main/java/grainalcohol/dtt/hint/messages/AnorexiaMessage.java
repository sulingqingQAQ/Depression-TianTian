package grainalcohol.dtt.hint.messages;

import grainalcohol.dtt.hint.SubscriptionHintMessage;
import grainalcohol.dtt.hint.timer.TimeUnit;
import grainalcohol.dtt.hint.timer.Timer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class AnorexiaMessage extends SubscriptionHintMessage {
    private static final Timer COOLDOWN_TIMER = Timer.of(3, TimeUnit.MINUTE);

    public AnorexiaMessage(ResourceLocation identifier) {
        super(identifier, true, 3);
    }

    @Override
    public @NotNull String getTranslationKey() {
        return "hint.dtt.anorexia";
    }

    @Override
    public @Nullable Consumer<ServerPlayer> getAfterSend() {
        return null;
    }

    @Override
    public @NotNull Timer getCooldownTimer() {
        return COOLDOWN_TIMER;
    }
}
