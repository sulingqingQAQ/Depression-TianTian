package grainalcohol.dtt.diary.dailystat.v2;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public class DailyStatKey<T> {
    private final ResourceLocation identifier;
    private final Class<T> type;

    public DailyStatKey(ResourceLocation identifier, Class<T> type) {
        this.identifier = identifier;
        this.type = type;
    }

    public ResourceLocation getIdentifier() {
        return identifier;
    }

    public Class<T> getType() {
        return type;
    }

    public static DailyStatKey<Boolean> createBooleanKey(ResourceLocation identifier) {
        return new DailyStatKey<>(identifier, Boolean.class);
    }

    public static DailyStatKey<Integer> createNumberKey(ResourceLocation identifier) {
        return new DailyStatKey<>(identifier, Integer.class);
    }

    @Override
    public String toString() {
        return getIdentifier().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DailyStatKey<?> that = (DailyStatKey<?>) o;
        return Objects.equals(getIdentifier(), that.getIdentifier()) && Objects.equals(getType(), that.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getType());
    }
}
