package grainalcohol.dtt.mixin.accessor;

import net.depression.mental.MentalStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = MentalStatus.class, remap = false)
public interface BoredomMapAccessor {
    @Accessor("boredom")
    ConcurrentHashMap<String, Integer> getBoredomMap();
}
