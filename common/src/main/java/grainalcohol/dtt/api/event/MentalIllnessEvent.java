package grainalcohol.dtt.api.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import grainalcohol.dtt.api.wrapper.MentalIllnessStatus;
import net.minecraft.server.level.ServerPlayer;

public interface MentalIllnessEvent {
    /**
     * 患病情况变化事件
     * @see MentalIllnessChangedEvent#onMentalIllnessChanged(ServerPlayer, MentalIllnessStatus, MentalIllnessStatus)
     */
    Event<MentalIllnessChangedEvent> MENTAL_HEALTH_CHANGED_EVENT = EventFactory.createLoop();

    /**
     * 患双相情感障碍时，情绪极性变化事件，进入躁狂相时isManicPhase为true，退出躁狂相时isManicPhase为false
      * @see MoodPolarityChanged#onMoodPolarityChanged(ServerPlayer, boolean)
     */
    Event<MoodPolarityChanged> MOOD_POLARITY_CHANGED_EVENT = EventFactory.createLoop();

    @FunctionalInterface
    interface MentalIllnessChangedEvent {
        void onMentalIllnessChanged(ServerPlayer player, MentalIllnessStatus lastTickStatus, MentalIllnessStatus currentStatus);
    }

    @FunctionalInterface
    interface MoodPolarityChanged {
        void onMoodPolarityChanged(ServerPlayer player, boolean isManicPhase);
    }
}
