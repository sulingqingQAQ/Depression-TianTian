package grainalcohol.dtt.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import grainalcohol.dtt.DTTMod;
import grainalcohol.dtt.effect.AnorexiaEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

public class DTTStatusEffect {
    // NeoForge 在 mod 构造时原版注册表已冻结，必须走 DeferredRegister；RegistrySupplier 本身就是 Holder<MobEffect>
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(DTTMod.MOD_ID, Registries.MOB_EFFECT);

    public static final RegistrySupplier<MobEffect> ANOREXIA = MOB_EFFECTS.register("anorexia", AnorexiaEffect::new);

    public static void init() {
        MOB_EFFECTS.register();
    }
}
