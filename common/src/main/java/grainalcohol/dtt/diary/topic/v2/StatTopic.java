package grainalcohol.dtt.diary.topic.v2;

import net.minecraft.resources.ResourceLocation;

public abstract class StatTopic extends Topic {
    public StatTopic(ResourceLocation identifier) {
        super(identifier, false);
    }

    @Override
    public double getDefaultWeight() {
        return 0.0;
    }
}
