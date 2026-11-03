package grainalcohol.dtt.registry;

import grainalcohol.dtt.DTTMod;
import grainalcohol.dtt.diary.dailystat.v2.DailyStatKey;
import grainalcohol.dtt.diary.topic.v2.EssentialTopic;
import grainalcohol.dtt.diary.topic.v2.StatTopic;
import grainalcohol.dtt.hint.HintMessage;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class DTTRegistries {
    public static final ResourceKey<Registry<DailyStatKey<Boolean>>> BOOLEAN_STAT_KEY_REGISTRY_KEY = ResourceKey.createRegistryKey(DTTMod.id("boolean_stat_key"));
    public static final ResourceKey<Registry<DailyStatKey<Integer>>> NUMBER_STAT_KEY_REGISTRY_KEY = ResourceKey.createRegistryKey(DTTMod.id("number_stat_key"));
    public static final ResourceKey<Registry<EssentialTopic>> ESSENTIAL_TOPIC_REGISTRY_KEY = ResourceKey.createRegistryKey(DTTMod.id("essential_topic"));
    public static final ResourceKey<Registry<StatTopic>> STAT_TOPIC_REGISTRY_KEY = ResourceKey.createRegistryKey(DTTMod.id("stat_topic"));
    public static final ResourceKey<Registry<HintMessage>> GLOBAL_HINT_MESSAGE_REGISTRY_KEY = ResourceKey.createRegistryKey(DTTMod.id("global_hint_message"));
    public static final ResourceKey<Registry<HintMessage>> HINT_MESSAGE_REGISTRY_KEY = ResourceKey.createRegistryKey(DTTMod.id("hint_message"));

    public static final Registry<DailyStatKey<Boolean>> BOOLEAN_STAT_KEY_REGISTRY;
    public static final Registry<DailyStatKey<Integer>> NUMBER_STAT_KEY_REGISTRY;
    public static final Registry<EssentialTopic> ESSENTIAL_TOPIC_REGISTRY;
    public static final Registry<StatTopic> STAT_TOPIC_REGISTRY;
    public static final Registry<HintMessage> GLOBAL_HINT_MESSAGE_REGISTRY;
    public static final Registry<HintMessage> HINT_MESSAGE_REGISTRY;

    // 这些注册表只在 DTT 内部当查找表用（不同步、不进存档），所以不挂进根注册表，两个平台行为一致
    static {
        BOOLEAN_STAT_KEY_REGISTRY = new MappedRegistry<>(BOOLEAN_STAT_KEY_REGISTRY_KEY, Lifecycle.stable());
        NUMBER_STAT_KEY_REGISTRY = new MappedRegistry<>(NUMBER_STAT_KEY_REGISTRY_KEY, Lifecycle.stable());
        ESSENTIAL_TOPIC_REGISTRY = new MappedRegistry<>(ESSENTIAL_TOPIC_REGISTRY_KEY, Lifecycle.stable());
        STAT_TOPIC_REGISTRY = new MappedRegistry<>(STAT_TOPIC_REGISTRY_KEY, Lifecycle.stable());
        GLOBAL_HINT_MESSAGE_REGISTRY = new MappedRegistry<>(GLOBAL_HINT_MESSAGE_REGISTRY_KEY, Lifecycle.stable());
        HINT_MESSAGE_REGISTRY = new MappedRegistry<>(HINT_MESSAGE_REGISTRY_KEY, Lifecycle.stable());
    }
}
