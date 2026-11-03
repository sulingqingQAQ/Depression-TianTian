package grainalcohol.dtt.diary.topic;

import grainalcohol.dtt.diary.dailystat.DailyStat;
import grainalcohol.dtt.diary.dailystat.DailyStatManager;
import grainalcohol.dtt.diary.feeling.Feeling;
import net.depression.item.diary.ConditionComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 顺序生成话题的方法是{@code generateTopicProcess()}
 * @see TopicProducer
 */
public class Topics {
    // TODO：不是这个也太屎山了，这我是真想重构，试试注册表模式
    // essential
    public static final Function<ServerPlayer, Topic> weather = player -> {
        boolean hasRain = DailyStatManager.getTodayDailyStat(player.getUUID()).isHasRained();
        // +2
        Topic topic = hasRain ?
                Topic.of(TopicType.ESSENTIAL, "weather_rain") :
                Topic.of(TopicType.ESSENTIAL, "weather_clear");
        // 下雨会让人感到孤独
        topic.setFeelingCompatibility(hasRain ? Feeling.LONELY : Feeling.WARM, 0.6);
        return topic;
    };
    public static final Function<ServerPlayer, Topic> petDied = player -> {
        // +1
        Topic topic = createTopicByBooleanDailyStat(
                player, TopicType.ESSENTIAL, DailyStat::isHasPetDied,
                "pet_died", true
        );
        // 宠物死亡会让人感到孤独、虚无和失败
        if (topic != null) {
            topic.setFeelingCompatibility(Feeling.LONELY, 0.6);
            topic.setFeelingCompatibility(Feeling.NIHILISTIC, 0.2);
            topic.setFeelingCompatibility(Feeling.FAILED, 0.8);
            topic.setShouldExcludedDueToNegativity(true);
        }
        return topic;
    };
    public static final Function<ServerPlayer, Topic> petBred = player -> {
        // +1
        Topic topic = createTopicByBooleanDailyStat(
                player, TopicType.ESSENTIAL,
                DailyStat::isHasPetBred, "pet_bred"
        );
        // 繁殖宠物会让人感到温暖和成功
        if (topic != null) {
            topic.setFeelingCompatibility(Feeling.WARM, 0.6);
            topic.setFeelingCompatibility(Feeling.SUCCESSFUL, 0.8);
        }
        return topic;
    };

    //stat
    public static final Function<ServerPlayer, Topic> animalBred = player -> {
        String name = new ConditionComponent(Stats.ANIMALS_BRED, 1, "animal_bred").get(player);
        Topic topic = !name.isEmpty() ? Topic.of(TopicType.STAT, name) : null;
        // 喂养动物会让人感到温暖和有希望
        if (topic != null) {
            topic.setFeelingCompatibility(Feeling.WARM, 0.2);
            topic.setFeelingCompatibility(Feeling.HOPEFUL, 0.4);
        }
        return topic;
    };
    public static final Function<ServerPlayer, Topic> monsterKilled = player -> {
        Topic topic = createTopicByDailyStat(
                player, DailyStat::getMonsterKilled, "monster_killed"
        );
        // 杀死怪物会让人感到成功
        topic.setFeelingCompatibility(Feeling.SUCCESSFUL, 0.2);
        return topic;
    };
    public static final Function<ServerPlayer, Topic> distanceMoved = player -> {
        Topic topic = createTopicByDailyStat(
                player, DailyStat::getDistanceMoved, "distance_moved"
        );
        // 行走距离这个太抽象了，也比较吃环境，就设置一点点符合度吧
        topic.setFeelingCompatibility(Feeling.SUCCESSFUL, 0.05);
        return topic;
    };
    public static final Function<ServerPlayer, Topic> damageTaken = player -> {
        Topic topic = createTopicByDailyStat(
                player, DailyStat::getDamageTaken, "damage_taken"
        );
        // 受到伤害会让人感到失败
        topic.setFeelingCompatibility(Feeling.FAILED, 0.2);
        return topic;
    };
    public static final Function<ServerPlayer, Topic> traded = player -> {
        Topic topic = createTopicByDailyStat(
                player, DailyStat::getTradedCount, "traded"
        );
        // 交易会让人感到成功
        topic.setFeelingCompatibility(Feeling.SUCCESSFUL, 0.1);
        return topic;
    };
    public static final Function<ServerPlayer, Topic> brewed = player -> {
        Topic topic = createTopicByDailyStat(
                player, DailyStat::getBrewedCount, "brewed"
        );
        // 酿造药水会让人感到有希望
        topic.setFeelingCompatibility(Feeling.HOPEFUL, 0.1);
        return topic;
    };
    public static final Function<ServerPlayer, Topic> flowerPotted = player -> {
        Topic topic = createTopicByBooleanDailyStat(
                player, DailyStat::isHasFlowerPotted, "flower_potted"
        );
        // 种花会让人感到温暖和有希望
        if (topic != null) {
            topic.setFeelingCompatibility(Feeling.WARM, 0.6);
            topic.setFeelingCompatibility(Feeling.HOPEFUL, 0.4);
        }
        return topic;
    };

    public static final Function<ServerPlayer, Topic> ate = player -> {
        Topic topic = createTopicByBooleanDailyStat(
                player, DailyStat::isHasAte, "ate"
        );
        // 进食会让人感到温暖和有希望
        if (topic != null) {
            topic.setFeelingCompatibility(Feeling.WARM, 0.2);
        }
        return topic;
    };

    // major impact
    public static final Function<ServerPlayer, Topic> enderDragonKilled = player -> {
        // +1
        Topic topic = createTopic(
                player, TopicType.MAJOR_IMPACT,
                p -> p.getStats().getValue(Stats.ENTITY_KILLED, EntityType.ENDER_DRAGON) > 0,
                "ender_dragon_killed", true
        );
        // 击杀末影龙会让人感到成功
        if (topic != null) {
            topic.setFeelingCompatibility(Feeling.HOPEFUL, 0.6);
            topic.setFeelingCompatibility(Feeling.SUCCESSFUL, 1.0);
        }
        return topic;
    };
    public static final Function<ServerPlayer, Topic> raidFailed = player -> {
        Topic topic = createTopicByBooleanDailyStat(
                player, TopicType.MAJOR_IMPACT,
                DailyStat::isHasRaidFailed, "raid_failed"
        );
        // 袭击失败会让人感到失败和虚无
        if (topic != null) {
            topic.setFeelingCompatibility(Feeling.FAILED, 0.6);
            topic.setFeelingCompatibility(Feeling.NIHILISTIC, 0.2);
            topic.setShouldExcludedDueToNegativity(true);
        }
        return topic;
    };
    public static final Function<ServerPlayer, Topic> raidWon = player -> {
        Topic topic = createTopicByBooleanDailyStat(
                player, TopicType.MAJOR_IMPACT,
                DailyStat::isHasRaidWon, "raid_won"
        );
        // 赢得袭击会让人感到成功和有希望
        if (topic != null) {
            topic.setFeelingCompatibility(Feeling.SUCCESSFUL, 0.6);
            topic.setFeelingCompatibility(Feeling.HOPEFUL, 0.2);
        }
        return topic;
    };

    @NotNull
    private static Topic createTopicByDailyStat(
            ServerPlayer player,
            Function<DailyStat, Integer> dataExtractor,
            String name
    ) {
        return createTopicByDailyStat(player, TopicType.STAT, dataExtractor, name);
    }

    @NotNull
    private static Topic createTopicByDailyStat(
            ServerPlayer player,
            TopicType topicType,
            Function<DailyStat, Integer> dataExtractor,
            String name
    ) {
        return createTopicByDailyStat(player, topicType, dataExtractor, name, false);
    }

    @NotNull
    private static Topic createTopicByDailyStat(
            ServerPlayer player,
            TopicType topicType,
            Function<DailyStat, Integer> dataExtractor,
            String name,
            boolean avoidRepetition
    ) {
        int today = dataExtractor.apply(DailyStatManager.getTodayDailyStat(player.getUUID()));
        int yesterday = dataExtractor.apply(DailyStatManager.getYesterdayDailyStat(player.getUUID()));
        int ema = dataExtractor.apply(DailyStatManager.getMovingAverageDailyStat(player.getUUID()));

        double weight = TopicWeightCalculator.calculateWeight(today, yesterday, ema);

        return Topic.of(topicType, name, weight, avoidRepetition);
    }

    @Nullable
    private static Topic createTopicByBooleanDailyStat(
            ServerPlayer player,
            Function<DailyStat, Boolean> dataExtractor,
            String name
    ) {
        return createTopicByBooleanDailyStat(player, TopicType.STAT, dataExtractor, name, false);
    }

    @Nullable
    private static Topic createTopicByBooleanDailyStat(
            ServerPlayer player,
            TopicType topicType,
            Function<DailyStat, Boolean> dataExtractor,
            String name
    ) {
        return createTopicByBooleanDailyStat(player, topicType, dataExtractor, name, false);
    }

    @Nullable
    private static Topic createTopicByBooleanDailyStat(
            ServerPlayer player,
            TopicType topicType,
            Function<DailyStat, Boolean> dataExtractor,
            String name,
            boolean avoidRepetition
    ) {
        return createTopic(player, topicType, p -> dataExtractor.apply(DailyStatManager.getTodayDailyStat(p.getUUID())), name, avoidRepetition);
    }

    @Nullable
    private static Topic createTopic(
            ServerPlayer player,
            TopicType topicType,
            Predicate<ServerPlayer> dataExtractor,
            String name,
            boolean avoidRepetition
    ) {
        boolean isHappened = dataExtractor.test(player);
        // 布尔类型话题无法计算权重，统一设为5.0
        return isHappened ? Topic.of(topicType, name, 5.0, avoidRepetition) : null;
    }
}
