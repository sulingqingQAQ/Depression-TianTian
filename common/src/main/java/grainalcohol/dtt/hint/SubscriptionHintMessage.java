package grainalcohol.dtt.hint;

import grainalcohol.dtt.hint.timer.Timer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public abstract class SubscriptionHintMessage extends HintMessage {
    public SubscriptionHintMessage(ResourceLocation identifier, boolean isImportant, int variantCount) {
        super(identifier, isImportant, variantCount);
    }

    public void trigger(ServerPlayer player, @NotNull Component... context) {
        HintMessageSender.trigger(player, this, context);
    }

    public void trigger(ServerPlayer player, @NotNull String... context) {
        HintMessageSender.trigger(player, this, context);
    }

    public void trigger(ServerPlayer player) {
        HintMessageSender.trigger(player, this);
    }

    @Override
    public final @Nullable Predicate<ServerPlayer> getGlobalCondition() {
        return null;
    }

    @Override
    public final boolean autoSend() {
        return false;
    }

    @Override
    public final @Nullable Timer getAccumulateTimer() {
        return null;
    }
}
