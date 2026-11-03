package grainalcohol.dtt.diary.topic.v2;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public abstract class Topic {
    private final ContextAttribute.Builder defaultContextAttributeBuilder;
    private final ResourceLocation identifier;
    private final boolean avoidRepetitionFromYesterday;

    public Topic(ResourceLocation identifier, boolean avoidRepetitionFromYesterday) {
        this.identifier = identifier;
        this.avoidRepetitionFromYesterday = avoidRepetitionFromYesterday;
        this.defaultContextAttributeBuilder = ContextAttribute.Builder
                .builder(getIdentifier().getPath(), getIdentifier())
                .weight(getDefaultWeight());
    }

    public ContextAttribute.Builder getDefaultContextAttributeBuilder() {
        return defaultContextAttributeBuilder;
    }

    public ResourceLocation getIdentifier() {
        return identifier;
    }

    public boolean shouldAvoidRepetitionFromYesterday() {
        return avoidRepetitionFromYesterday;
    }

    public abstract double getDefaultWeight();

    public abstract Optional<ContextAttribute> getAttribute(ServerPlayer player, boolean gentleMode);
}
