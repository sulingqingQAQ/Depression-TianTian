package grainalcohol.dtt.diary.dailystat.v2;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;

public class DailyStatManager {
    // NBT keys
    public static final String DAILY_STAT_NBT_KEY = "DTTDailyStat";
    public static final String TODAY_DAILY_STAT_NBT_KEY = "Today";
    public static final String YESTERDAY_DAILY_STAT_NBT_KEY = "Yesterday";
    public static final String MOVING_AVERAGE_DAILY_STAT_NBT_KEY = "MovingAverage";

    private static final Map<UUID, DailyStat> todayStats = new ConcurrentHashMap<>();
    private static final Map<UUID, DailyStat> yesterdayStats = new ConcurrentHashMap<>();
    private static final Map<UUID, DailyStat> EMAStats = new ConcurrentHashMap<>();

    public static void updateDailyStat(UUID playerId, double EMAFactor) {
        // 日期更新时：
        DailyStat todayStat = todayStats.get(playerId);

        if (todayStat == null) return;

        // 覆盖昨天的数据
        // 用 put 而不是 replace：首次跨天时 yesterdayStats 里还没有该玩家的键，replace 会静默失败导致昨日统计永久为空
        yesterdayStats.put(playerId, todayStat);

        // 将当前的数据用于计算移动平均值
        DailyStat previousEMA = EMAStats.get(playerId);
        if (previousEMA == null) {
            previousEMA = new DailyStat();
        }

        // EMA因子越大，新数据越重要，旧数据越不重要
        DailyStat newEMA = new DailyStat(todayStat);
        // 新数据
        newEMA.multiply(EMAFactor);
        // 旧数据
        previousEMA.multiply(1 - EMAFactor);

        // 合并并覆盖
        newEMA.merge(previousEMA);
        // 同 yesterdayStats：首次计算 EMA 时键不存在，replace 会丢掉这次结果，必须用 put
        EMAStats.put(playerId, newEMA);

        // 清空今天的数据
        // 同前两处：首次跨天时 todayStats 可能已被清过，replace 不生效会让统计跨天累积
        todayStats.put(playerId, new DailyStat());
    }

    public static DailyStat getTodayStat(UUID playerId) {
        todayStats.computeIfAbsent(playerId, k -> new DailyStat());
        return todayStats.get(playerId);
    }

    public static DailyStat getYesterdayStat(UUID playerId) {
        yesterdayStats.computeIfAbsent(playerId, k -> new DailyStat());
        return yesterdayStats.get(playerId);
    }

    public static DailyStat getEMAStat(UUID playerId) {
        EMAStats.computeIfAbsent(playerId, k -> new DailyStat());
        return EMAStats.get(playerId);
    }

    /**
     * 把该玩家的三层统计投影进 NBT（纯读）。<br>
     * 缺失哪一层就跳过哪一层的键，绝不在序列化里 get-or-create：旧实现用 {@code getTodayStat / getYesterdayStat / getEMAStat}
     * 会顺手 {@code computeIfAbsent} 造出全零 {@code DailyStat} 并写进存档，正是"退出时二次序列化写出默认值"的根因之一。
     * 读侧（{@link #readFromNbt}）本来就按子键独立宽容，所以缺键不会让存档读不回来。
     */
    public static void writeToNbt(UUID playerId, CompoundTag nbt) {
        CompoundTag dailyStatNbt = new CompoundTag();
        boolean written = false;

        DailyStat todayStat = todayStats.get(playerId);
        if (todayStat != null) {
            CompoundTag todayNbt = new CompoundTag();
            todayStat.writeToNbt(todayNbt);
            dailyStatNbt.put(TODAY_DAILY_STAT_NBT_KEY, todayNbt);
            written = true;
        }

        DailyStat yesterdayStat = yesterdayStats.get(playerId);
        if (yesterdayStat != null) {
            CompoundTag yesterdayNbt = new CompoundTag();
            yesterdayStat.writeToNbt(yesterdayNbt);
            dailyStatNbt.put(YESTERDAY_DAILY_STAT_NBT_KEY, yesterdayNbt);
            written = true;
        }

        DailyStat emaStat = EMAStats.get(playerId);
        if (emaStat != null) {
            CompoundTag emaNbt = new CompoundTag();
            emaStat.writeToNbt(emaNbt);
            dailyStatNbt.put(MOVING_AVERAGE_DAILY_STAT_NBT_KEY, emaNbt);
            written = true;
        }

        // 一层都没有（状态已被清理）时整节不写，保持"缺哪个包的状态就不写哪个包的键"
        if (!written) return;

        nbt.put(DAILY_STAT_NBT_KEY, dailyStatNbt);
    }

    /**
     * 清理该玩家的全部内存统计（今日/昨日/EMA）。<br>
     * 只应在"该次登出的存档已经写完"之后调用（{@code PlayerList.remove} 的 RETURN，见
     * {@code grainalcohol.dtt.init.DTTPlayerState#evict}），否则会丢数据。幂等。
     */
    public static void remove(UUID playerId) {
        todayStats.remove(playerId);
        yesterdayStats.remove(playerId);
        EMAStats.remove(playerId);
    }

    /** 清空全部玩家的内存统计（服务器停止 / 开始前的全量清扫，见 {@code DTTPlayerState#resetAll}）。 */
    public static void clearAll() {
        todayStats.clear();
        yesterdayStats.clear();
        EMAStats.clear();
    }

    public static void readFromNbt(UUID playerId, CompoundTag nbt) {
        if (!nbt.contains(DAILY_STAT_NBT_KEY)) return;

        CompoundTag dailyStatNbt = nbt.getCompound(DAILY_STAT_NBT_KEY);

        if (dailyStatNbt.contains(TODAY_DAILY_STAT_NBT_KEY)) {
            DailyStat todayStat = new DailyStat();
            todayStat.readFromNbt(dailyStatNbt.getCompound(TODAY_DAILY_STAT_NBT_KEY));
            todayStats.put(playerId, todayStat);
        }

        if (dailyStatNbt.contains(YESTERDAY_DAILY_STAT_NBT_KEY)) {
            DailyStat yesterdayStat = new DailyStat();
            yesterdayStat.readFromNbt(dailyStatNbt.getCompound(YESTERDAY_DAILY_STAT_NBT_KEY));
            yesterdayStats.put(playerId, yesterdayStat);
        }

        if (dailyStatNbt.contains(MOVING_AVERAGE_DAILY_STAT_NBT_KEY)) {
            DailyStat emaStat = new DailyStat();
            emaStat.readFromNbt(dailyStatNbt.getCompound(MOVING_AVERAGE_DAILY_STAT_NBT_KEY));
            EMAStats.put(playerId, emaStat);
        }
    }
}
