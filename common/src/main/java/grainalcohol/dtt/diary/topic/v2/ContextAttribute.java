package grainalcohol.dtt.diary.topic.v2;

import grainalcohol.dtt.diary.feeling.v2.Feeling;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ContextAttribute {
    private final String translationKey;
    private final double weight;
    private final boolean extremeNegativity;
    private final Map<Feeling, Double> feelingCompatibilityMap;
    private final ResourceLocation sourceTopicIdentifier;

    private ContextAttribute(Builder builder) {
        this.translationKey = builder.translationKey;
        this.weight = builder.weight;
        this.extremeNegativity = builder.extremeNegativity;
        this.feelingCompatibilityMap = builder.feelingCompatibilityMap;
        this.sourceTopicIdentifier = builder.sourceTopicIdentifier;
    }

    private ContextAttribute(ContextAttribute other) {
        this.translationKey = other.translationKey;
        this.weight = other.weight;
        this.extremeNegativity = other.extremeNegativity;
        this.feelingCompatibilityMap = new HashMap<>(other.feelingCompatibilityMap);
        this.sourceTopicIdentifier = other.sourceTopicIdentifier;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public ContextAttribute copy() {
        return new ContextAttribute(this);
    }

    public double getFeelingCompatibility(Feeling feeling) {
        return feelingCompatibilityMap.get(feeling);
    }

    public double getWeight() {
        return weight;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean isExtremeNegativity() {
        return extremeNegativity;
    }

    public ResourceLocation getSourceTopicIdentifier() {
        return sourceTopicIdentifier;
    }

    public static class Builder {
        private String translationKey = "context.default";
        private double weight = 0.0;
        private boolean extremeNegativity = false;
        private Map<Feeling, Double> feelingCompatibilityMap = new HashMap<>();
        private ResourceLocation sourceTopicIdentifier;

        private Builder(ContextAttribute contextAttribute) {
            this.translationKey = contextAttribute.translationKey;
            this.weight = contextAttribute.weight;
            this.extremeNegativity = contextAttribute.extremeNegativity;
            this.feelingCompatibilityMap = new HashMap<>(contextAttribute.feelingCompatibilityMap);
            this.sourceTopicIdentifier = contextAttribute.sourceTopicIdentifier;
        }

        private Builder() {
            for (Feeling feeling : Feeling.values()) {
                feelingCompatibilityMap.put(feeling, 0.0);
            }
        }

        public static Builder builder(String translationKey, ResourceLocation sourceTopicIdentifier) {
            return new Builder().translationKey(translationKey).sourceTopicIdentifier(sourceTopicIdentifier);
        }

        public static Builder builder(ResourceLocation sourceTopicIdentifier) {
            return new Builder().sourceTopicIdentifier(sourceTopicIdentifier);
        }

        public Builder sourceTopicIdentifier(ResourceLocation sourceTopicIdentifier) {
            this.sourceTopicIdentifier = sourceTopicIdentifier;
            return this;
        }

        public Builder translationKey(String translationKey) {
            this.translationKey = translationKey;
            return this;
        }

        public Builder weight(double weight) {
            this.weight = Mth.clamp(weight, -10.0, 10.0);
            return this;
        }

        public Builder extremeNegativity() {
            this.extremeNegativity = true;
            return this;
        }

        public Builder feelingCompatibility(Map<Feeling, Double> compatibilityMap) {
            this.feelingCompatibilityMap = compatibilityMap;
            return this;
        }

        public Builder feelingCompatibility(Feeling feeling, double compatibility) {
            this.feelingCompatibilityMap.put(feeling, Mth.clamp(compatibility, 0.0, 1.0));
            return this;
        }

        public ContextAttribute build() {
            return new ContextAttribute(this);
        }
    }
}
