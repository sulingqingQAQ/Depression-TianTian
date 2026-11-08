package grainalcohol.dtt.diary.topic.v2;

import grainalcohol.dtt.registry.DTTRegistries;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public class TopicManager {
    private static final Map<UUID, List<EssentialTopic>> historicalEssentialTopicsMap = new HashMap<>();
    public static final String HISTORICAL_ESSENTIAL_TOPICS_KEY = "HistoricalEssentialTopics";

    public static void recordTopic(UUID playerUUID, EssentialTopic topic) {
        historicalEssentialTopicsMap.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(topic);
    }

    public static List<EssentialTopic> getHistoricalTopics(UUID playerUUID) {
        historicalEssentialTopicsMap.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        return historicalEssentialTopicsMap.get(playerUUID);
    }

    /**
     * 清理该玩家的历史话题记录。
     * 只应在"该次登出的存档已经写完"之后调用（{@code PlayerList.remove} 的 RETURN，见
     * {@code grainalcohol.dtt.init.DTTPlayerState#evict}），否则会丢数据。幂等。
     */
    public static void remove(UUID playerUUID) {
        historicalEssentialTopicsMap.remove(playerUUID);
    }

    /** 清空全部玩家的历史话题记录（服务器停止 / 开始前的全量清扫，见 {@code DTTPlayerState#resetAll}）。 */
    public static void clearAll() {
        historicalEssentialTopicsMap.clear();
    }

    /**
     * 把该玩家的历史话题投影进 NBT（纯读）。<br>
     * 用 {@code get} 而不是 {@link #getHistoricalTopics}：后者会 {@code computeIfAbsent} 造出空表并留在内存里
     * （旧实现的副作用之一）。缺失或为空就不写这个键，读侧对缺键宽容。
     */
    public static void writeToNbt(UUID playerUUID, CompoundTag nbt) {
        List<EssentialTopic> historicalEssentialTopics = historicalEssentialTopicsMap.get(playerUUID);
        if (historicalEssentialTopics == null || historicalEssentialTopics.isEmpty()) return;

        ListTag nbtList = new ListTag();
        for (EssentialTopic essentialTopic: historicalEssentialTopics) {
            nbtList.add(StringTag.valueOf(essentialTopic.getIdentifier().toString()));
        }
        nbt.put(HISTORICAL_ESSENTIAL_TOPICS_KEY, nbtList);
    }

    public static void readFromNbt(UUID playerUUID, CompoundTag nbt) {
        if (!nbt.contains(HISTORICAL_ESSENTIAL_TOPICS_KEY)) return;

        ListTag nbtList = nbt.getList(HISTORICAL_ESSENTIAL_TOPICS_KEY, Tag.TAG_STRING);
        List<EssentialTopic> toAddList = new ArrayList<>();
        for (Tag nbtElement : nbtList) {
            String idStr = nbtElement.getAsString();
            ResourceLocation identifier = ResourceLocation.tryParse(idStr);
            if (identifier == null) continue;

            EssentialTopic essentialTopic = DTTRegistries.ESSENTIAL_TOPIC_REGISTRY.get(identifier);
            if (essentialTopic != null) {
                toAddList.add(essentialTopic);
            }
        }
        historicalEssentialTopicsMap.put(playerUUID, toAddList);
    }
}
