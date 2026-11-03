package grainalcohol.dtt.diary.topic.v2.topics;

import grainalcohol.dtt.diary.feeling.v2.Feeling;
import grainalcohol.dtt.diary.topic.TopicWeightCalculator;
import grainalcohol.dtt.diary.topic.v2.StatTopic;
import grainalcohol.dtt.diary.topic.v2.ContextAttribute;
import grainalcohol.dtt.init.DTTDailyStat;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class MonsterKilledTopic extends StatTopic {
    public MonsterKilledTopic(ResourceLocation identifier) {
        super(identifier);
    }

    @Override
    public Optional<ContextAttribute> getAttribute(ServerPlayer player, boolean gentleMode) {
        ContextAttribute monsterKilled = getDefaultContextAttributeBuilder()
                .translationKey("killed_monster")
                .weight(TopicWeightCalculator.calculateWeight(player.getUUID(), DTTDailyStat.MONSTER_KILLED))
                .feelingCompatibility(Feeling.SUCCESSFUL, 0.6)
                .feelingCompatibility(Feeling.WARM, 0.05)
                .build();

        return Optional.of(monsterKilled);
    }
}
