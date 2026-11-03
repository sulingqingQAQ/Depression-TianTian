package grainalcohol.dtt.diary.topic.v2.topics;

import grainalcohol.dtt.diary.dailystat.v2.DailyStatManager;
import grainalcohol.dtt.diary.feeling.v2.Feeling;
import grainalcohol.dtt.diary.topic.v2.ContextAttribute;
import grainalcohol.dtt.diary.topic.v2.StatTopic;
import grainalcohol.dtt.init.DTTDailyStat;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class FlowerPottedTopic extends StatTopic {
    public FlowerPottedTopic(ResourceLocation identifier) {
        super(identifier);
    }

    @Override
    public Optional<ContextAttribute> getAttribute(ServerPlayer player, boolean gentleMode) {
        boolean condition = DailyStatManager.getTodayStat(player.getUUID()).getBooleanStat(DTTDailyStat.FLOWER_POTTED);

        ContextAttribute flowerPotted = getDefaultContextAttributeBuilder()
                .translationKey("flower_potted")
                .weight(2.0)
                .feelingCompatibility(Feeling.WARM, 0.2)
                .build();

        return condition ? Optional.of(flowerPotted) : Optional.empty();
    }
}
