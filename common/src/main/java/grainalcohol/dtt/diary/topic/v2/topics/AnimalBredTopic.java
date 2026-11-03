package grainalcohol.dtt.diary.topic.v2.topics;

import grainalcohol.dtt.diary.feeling.v2.Feeling;
import grainalcohol.dtt.diary.topic.TopicWeightCalculator;
import grainalcohol.dtt.diary.topic.v2.ContextAttribute;
import grainalcohol.dtt.diary.topic.v2.StatTopic;
import grainalcohol.dtt.init.DTTDailyStat;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class AnimalBredTopic extends StatTopic {
    public AnimalBredTopic(ResourceLocation identifier) {
        super(identifier);
    }

    @Override
    public Optional<ContextAttribute> getAttribute(ServerPlayer player, boolean gentleMode) {
        ContextAttribute animalBred = getDefaultContextAttributeBuilder()
                .translationKey("animal_bred")
                .weight(TopicWeightCalculator.calculateWeight(player.getUUID(), DTTDailyStat.ANIMAL_BRED))
                .feelingCompatibility(Feeling.WARM, 0.2)
                .feelingCompatibility(Feeling.FAILED, -0.1)
                .build();

        return Optional.of(animalBred);
    }
}
