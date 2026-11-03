package grainalcohol.dtt.diary.topic.v2.topics;

import grainalcohol.dtt.diary.feeling.v2.Feeling;
import grainalcohol.dtt.diary.topic.TopicWeightCalculator;
import grainalcohol.dtt.diary.topic.v2.ContextAttribute;
import grainalcohol.dtt.diary.topic.v2.StatTopic;
import grainalcohol.dtt.init.DTTDailyStat;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class PotionBrewedTopic extends StatTopic {
    public PotionBrewedTopic(ResourceLocation identifier) {
        super(identifier);
    }

    @Override
    public Optional<ContextAttribute> getAttribute(ServerPlayer player, boolean gentleMode) {
        ContextAttribute brewed = getDefaultContextAttributeBuilder()
                .translationKey("potion_brewed")
                .weight(TopicWeightCalculator.calculateWeight(player.getUUID(), DTTDailyStat.POTION_BREWED))
                .feelingCompatibility(Feeling.SUCCESSFUL, 0.2)
                .feelingCompatibility(Feeling.WARM, 0.01)
                .build();

        return Optional.of(brewed);
    }
}
