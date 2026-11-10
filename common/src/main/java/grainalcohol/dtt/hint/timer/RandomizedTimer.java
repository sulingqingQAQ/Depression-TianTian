package grainalcohol.dtt.hint.timer;

import grainalcohol.dtt.util.MathUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;

public class RandomizedTimer extends Timer {
    private static final Random RANDOM = new Random();
    private final int randomBound;
    private final TimeUnit randomBoundUnit;

    public RandomizedTimer(grainalcohol.dtt.hint.timer.RandomizedTimer.Builder builder) {
        super(builder);
        this.randomBound = builder.randomBound;
        this.randomBoundUnit = builder.randomBoundUnit;
    }

    @Override
    public RandomizedTimer.Builder toBuilder() {
        return RandomizedTimer.Builder.builder(getTime(), getTimeUnit())
                .extraTime(getExtraTime(), getExtraTimeUnit())
                .randomBound(getRandomBound(), getRandomBoundUnit())
                .condition(getCondition());
    }

    @Override
    public Timer copy() {
        return toBuilder().build();
    }

    @Override
    public void reset() {
        reset(RANDOM);
    }

    /**
     * 按给定随机源复位：基准时长加 {@link MathUtil#inRange(Random, int)} 的偏移，即 [-randomBound, randomBound-1] 个 randomBoundUnit；
     * randomBound 为 0 表示不随机，直接复位到基准时长。包内可见，供测试用可控随机值驱动真实复位逻辑。
     */
    void reset(Random random) {
        int offset = randomBound == 0 ? 0 : MathUtil.inRange(random, randomBound);
        super.setTicks(getTime() * getTimeUnit().getDurationTicks() + offset * randomBoundUnit.getDurationTicks());
    }

    /**
     * 最大合法剩余 tick = 基准时长 + 最大正偏移（(randomBound-1) 个 randomBoundUnit）。<br>
     * 原来沿用父类的“只含基准时长”，{@link #reset()} 产生的正偏移会被 {@link #setTicks(int)} 截掉，
     * 10 秒±5 秒的轮询只剩 5–10 秒且一半落在 10 秒；NBT 读回的剩余 tick 也同样被截。
     */
    @Override
    public int getMaxTicks() {
        int maxPositiveOffset = randomBound == 0 ? 0 : (randomBound - 1) * randomBoundUnit.getDurationTicks();
        return super.getMaxTicks() + maxPositiveOffset;
    }

    @Override
    public Timer withCondition(@NotNull Predicate<ServerPlayer> condition) {
        return toBuilder().condition(condition).build();
    }

    public int getRandomBound() {
        return randomBound;
    }

    public TimeUnit getRandomBoundUnit() {
        return randomBoundUnit;
    }

    public static class Builder extends Timer.Builder {
        private int randomBound = 0;
        private TimeUnit randomBoundUnit = TimeUnit.TICK;

        public Builder(int time, TimeUnit timeUnit) {
            super(time, timeUnit);
        }

        public static RandomizedTimer.Builder builder(int time, TimeUnit timeUnit) {
            return new grainalcohol.dtt.hint.timer.RandomizedTimer.Builder(time, timeUnit);
        }

        @Override
        public RandomizedTimer.Builder timeTicks(int timeTicks) {
            return time(timeTicks, TimeUnit.TICK);
        }

        @Override
        public RandomizedTimer.Builder time(int time, TimeUnit timeUnit) {
            super.time(time, timeUnit);
            return this;
        }

        @Override
        public RandomizedTimer.Builder extraTicks(int extraTicks) {
            return extraTime(extraTicks, TimeUnit.TICK);
        }

        @Override
        public RandomizedTimer.Builder extraTime(int extraTime, TimeUnit extraTimeUnit) {
            super.extraTime(extraTime, extraTimeUnit);
            return this;
        }

        @Override
        public RandomizedTimer.Builder condition(@NotNull Predicate<ServerPlayer> condition) {
            super.condition(condition);
            return this;
        }

        public RandomizedTimer.Builder randomBoundTicks(int randomBoundTicks) {
            return randomBound(randomBoundTicks, TimeUnit.TICK);
        }

        public RandomizedTimer.Builder randomBound(int randomBound, TimeUnit randomBoundUnit) {
            if (randomBound < 0) {
                throw new IllegalArgumentException("randomBound must be >= 0 (0 = no randomness), got " + randomBound);
            }
            this.randomBound = randomBound;
            this.randomBoundUnit = randomBoundUnit;
            return this;
        }

        @Override
        public RandomizedTimer build() {
            return new RandomizedTimer(this);
        }
    }
}
