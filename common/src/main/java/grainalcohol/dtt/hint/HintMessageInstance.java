package grainalcohol.dtt.hint;

import grainalcohol.dtt.hint.timer.Timer;
import grainalcohol.dtt.registry.DTTRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class HintMessageInstance implements Sendable {
    /**
     * 积累判定与发送判定的降频间隔（tick）。
     * <p>积累计时器的条件里常带重活（JukeboxMessage 的 405 次方块实体查询、PetMessage 的 AABB 搜索 + 视线裁剪）。
     * 冷却计时器一旦走完（{@code isFinished()} 恒为 true），原来这些条件就会每 tick 永久执行一遍；
     * 改为每 20 tick 判定一次，并让积累计时器按 20 tick 步进，消息的实际时长与冷却时长都不变。</p>
     */
    private static final int THROTTLED_CHECK_INTERVAL = 20;

    @NotNull
    private final HintMessage hintMessage;
    @NotNull
    private final Timer cooldownTimer;
    @Nullable
    private final Timer accumulateTimer;
    // 距下次降频判定的剩余tick，只在需要判定时才归零，避免每 tick 取模
    private int ticksUntilThrottledCheck = THROTTLED_CHECK_INTERVAL;

    public HintMessageInstance(HintMessage hintMessage) {
        this.hintMessage = hintMessage;
        this.cooldownTimer = hintMessage.getCooldownTimer().copy();
        this.accumulateTimer = hintMessage.getAccumulateTimer() == null ? null : hintMessage.getAccumulateTimer().copy();
    }

    @Nullable
    public Timer getAccumulateTimer() {
        return accumulateTimer;
    }

    @NotNull
    public Timer getCooldownTimer() {
        return cooldownTimer;
    }

    @Override
    public @NotNull HintMessage asHintMessage() {
        return hintMessage;
    }

    public void tick(ServerPlayer player) {
        if (getGlobalCondition() != null && !getGlobalCondition().test(player)) return;

        // 冷却计时器照常每 tick 推进，冷却时长保持不变
        getCooldownTimer().tick(player);

        if (--ticksUntilThrottledCheck > 0) return; // 未到降频点，本 tick 不做重条件判定
        ticksUntilThrottledCheck = THROTTLED_CHECK_INTERVAL;

        // 积累判定与发送判定每 THROTTLED_CHECK_INTERVAL tick 做一次；积累计时器按经过的tick步进，
        // 所以"在唱片机/宠物旁待3秒"这类时长的语义不变，只是判定粒度从 1 tick 放宽到 20 tick
        if (getAccumulateTimer() != null && getCooldownTimer().isFinished()) {
            getAccumulateTimer().tick(player, THROTTLED_CHECK_INTERVAL);
        }

        if (asHintMessage().autoSend() && canSend()) send(player);
    }

    private boolean canSend() {
        return (getAccumulateTimer() == null || getAccumulateTimer().isFinished()) && getCooldownTimer().isFinished();
    }

    @Override
    public @NotNull ResourceLocation getIdentifier() {
        return asHintMessage().getIdentifier();
    }

    @Override
    public void send(ServerPlayer player) {
        // afterSend 由 HintMessageSender 在提示实际显示时执行（重要消息即刻、普通消息出队显示时），这里不再另执行一次
        HintMessageSender.send(player, this);

        resetTimer();
    }

    public void resetTimer() {
        getCooldownTimer().reset();
        if (getAccumulateTimer() != null) getAccumulateTimer().reset();
    }

    @Override
    public @NotNull String getTranslationKey() {
        return asHintMessage().getTranslationKey();
    }

    @Override
    public int getVariantCount() {
        return asHintMessage().getVariantCount();
    }

    @Override
    public boolean isImportant() {
        return asHintMessage().isImportant();
    }

    @Override
    public @NotNull String[] getContext() {
        return new String[0];
    }

    @Nullable
    public Predicate<ServerPlayer> getGlobalCondition() {
        return asHintMessage().getGlobalCondition();
    }

    @Nullable
    public Consumer<ServerPlayer> getAfterSend() {
        return asHintMessage().getAfterSend();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Identifier", getIdentifier().toString());
        nbt.putInt("CooldownTimerTicks", getCooldownTimer().getTicks());
        if (getAccumulateTimer() != null) {
            nbt.putInt("AccumulateTimerTicks", getAccumulateTimer().getTicks());
        }
        return nbt;
    }

    public static HintMessageInstance fromNbt(CompoundTag nbt) {
        ResourceLocation identifier = ResourceLocation.parse(nbt.getString("Identifier"));
        HintMessage hintMessage = DTTRegistries.GLOBAL_HINT_MESSAGE_REGISTRY.get(identifier);
        if (hintMessage == null) throw new IllegalStateException("No HintMessage found for identifier: " + identifier);

        HintMessageInstance instance = hintMessage.createInstance();
        instance.getCooldownTimer().setTicks(nbt.getInt("CooldownTimerTicks"));
        if (nbt.contains("AccumulateTimerTicks") && instance.getAccumulateTimer() != null) {
            instance.getAccumulateTimer().setTicks(nbt.getInt("AccumulateTimerTicks"));
        }
        return instance;
    }

    @Override
    public String toString() {
        return "HintMessageInstance(" + getIdentifier() + ")";
    }
}
