package grainalcohol.dtt.hint.messages;

import grainalcohol.dtt.hint.SimpleHintMessage;
import grainalcohol.dtt.hint.timer.TimeUnit;
import grainalcohol.dtt.hint.timer.Timer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class DarknessMessage extends SimpleHintMessage {
    private static final Timer COOLDOWN_TIMER = Timer.Builder.builder(3, TimeUnit.GAME_HOUR)
            // 亮度大于7开始处理冷却
            .condition(player -> player.serverLevel().getMaxLocalRawBrightness(player.blockPosition()) > 7)
            .build();
    private static final Timer ACCUMULATE_TIMER = Timer.Builder.builder(30, TimeUnit.SECOND)
            // 亮度小于4开始积累
            .condition(player -> player.serverLevel().getMaxLocalRawBrightness(player.blockPosition()) < 4)
            .build();

    public DarknessMessage(ResourceLocation identifier) {
        super(identifier, true, 3);
    }

    @Override
    public @Nullable Predicate<ServerPlayer> getGlobalCondition() {
        return null;
    }

    @Override
    public @NotNull String getTranslationKey() {
        return "hint.dtt.darkness";
    }

    @Override
    public @Nullable Consumer<ServerPlayer> getAfterSend() {
        return null;
    }

    @Override
    public @NotNull Timer getCooldownTimer() {
        return COOLDOWN_TIMER;
    }

    @Override
    public @Nullable Timer getAccumulateTimer() {
        return ACCUMULATE_TIMER;
    }
}
