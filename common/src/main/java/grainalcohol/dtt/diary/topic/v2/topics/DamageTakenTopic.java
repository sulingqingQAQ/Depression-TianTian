package grainalcohol.dtt.diary.topic.v2.topics;

import grainalcohol.dtt.diary.feeling.v2.Feeling;
import grainalcohol.dtt.diary.topic.TopicWeightCalculator;
import grainalcohol.dtt.diary.topic.v2.ContextAttribute;
import grainalcohol.dtt.diary.topic.v2.StatTopic;
import grainalcohol.dtt.init.DTTDailyStat;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class DamageTakenTopic extends StatTopic {
    public DamageTakenTopic(ResourceLocation identifier) {
        super(identifier);
    }

    @Override
    public Optional<ContextAttribute> getAttribute(ServerPlayer player, boolean gentleMode) {
        ContextAttribute damageTaken = getDefaultContextAttributeBuilder()
                .translationKey("damage_taken")
                .weight(TopicWeightCalculator.calculateWeight(player.getUUID(), DTTDailyStat.DAMAGE_TAKEN))
                .feelingCompatibility(Feeling.FAILED, 0.6)
                .feelingCompatibility(Feeling.NIHILISTIC, 0.02)
                .build();
        return Optional.of(damageTaken);
    }
}
