package grainalcohol.dtt.diary.topic.v2.topics;

import grainalcohol.dtt.diary.dailystat.v2.DailyStatManager;
import grainalcohol.dtt.diary.feeling.v2.Feeling;
import grainalcohol.dtt.diary.topic.v2.EssentialTopic;
import grainalcohol.dtt.diary.topic.v2.ContextAttribute;
import grainalcohol.dtt.init.DTTDailyStat;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class WeatherTopic extends EssentialTopic {
    public WeatherTopic(ResourceLocation identifier) {
        super(identifier, false);
    }

    @Override
    public Optional<ContextAttribute> getAttribute(ServerPlayer player, boolean gentleMode) {
        boolean hasRained = DailyStatManager.getTodayStat(player.getUUID()).getBooleanStat(DTTDailyStat.RAINED);

        ContextAttribute weatherRain = getDefaultContextAttributeBuilder()
                .translationKey("weather_rain")
                .feelingCompatibility(Feeling.NIHILISTIC, 0.2)
                .build();
        ContextAttribute weatherClear = getDefaultContextAttributeBuilder()
                .translationKey("weather_clear")
                .feelingCompatibility(Feeling.WARM, 0.2)
                .build();

        if (hasRained) {
            return Optional.of(weatherRain);
        } else {
            return Optional.of(weatherClear);
        }
    }
}
