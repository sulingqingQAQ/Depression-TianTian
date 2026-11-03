package grainalcohol.dtt.diary.topic.v2;

import net.minecraft.resources.ResourceLocation;

public abstract class EssentialTopic extends Topic {
    public EssentialTopic(ResourceLocation identifier, boolean avoidRepetitionFromYesterday) {
        super(identifier, avoidRepetitionFromYesterday);
    }

     @Override
    public double getDefaultWeight() {
        return 8.0;
    }
}
