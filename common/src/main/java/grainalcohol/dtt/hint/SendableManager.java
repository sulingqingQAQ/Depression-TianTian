package grainalcohol.dtt.hint;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;

public class SendableManager<S extends Sendable> implements Iterable<Map.Entry<ResourceLocation, S>> {
    private final Map<ResourceLocation, S> sendableMap = new ConcurrentHashMap<>();

    /**
     * 不会替换已存在的实例，如果需要替换请使用 replace 方法
     * @param sendable sendable 实例
     */
    public void register(S sendable) {
        if (!containsKey(sendable.getIdentifier())) {
            sendableMap.put(sendable.getIdentifier(), sendable);
        }
    }

    public void replace(S sendable) {
        sendableMap.replace(sendable.getIdentifier(), sendable);
    }

    public S get(ResourceLocation identifier) {
        return sendableMap.get(identifier);
    }

    public boolean containsKey(ResourceLocation identifier) {
        return sendableMap.containsKey(identifier);
    }

    public void remove(ResourceLocation identifier) {
        sendableMap.remove(identifier);
    }

    public Set<Map.Entry<ResourceLocation, S>> entrySet() {
        return sendableMap.entrySet();
    }

    public Set<ResourceLocation> keySet() {
        return sendableMap.keySet();
    }

    public Collection<S> values() {
        return sendableMap.values();
    }

    @Override
    public @NotNull Iterator<Map.Entry<ResourceLocation, S>> iterator() {
        return sendableMap.entrySet().iterator();
    }

    public void forEach(@NotNull BiConsumer<ResourceLocation, S> action) {
        sendableMap.forEach(action);
    }
}
