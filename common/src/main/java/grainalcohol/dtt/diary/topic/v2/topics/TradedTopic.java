package grainalcohol.dtt.diary.topic.v2.topics;

import grainalcohol.dtt.diary.feeling.v2.Feeling;
import grainalcohol.dtt.diary.topic.TopicWeightCalculator;
import grainalcohol.dtt.diary.topic.v2.ContextAttribute;
import grainalcohol.dtt.diary.topic.v2.StatTopic;
import grainalcohol.dtt.init.DTTDailyStat;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class TradedTopic extends StatTopic {
    public TradedTopic(ResourceLocation identifier) {
        super(identifier);
    }

    @Override
    public Optional<ContextAttribute> getAttribute(ServerPlayer player, boolean gentleMode) {
        ContextAttribute traded = getDefaultContextAttributeBuilder()
                .translationKey("traded")
                .weight(TopicWeightCalculator.calculateWeight(player.getUUID(), DTTDailyStat.TRADED_COUNT))
                .feelingCompatibility(Feeling.SUCCESSFUL, 0.6)
                .build();

        return Optional.of(traded);
    }
}
