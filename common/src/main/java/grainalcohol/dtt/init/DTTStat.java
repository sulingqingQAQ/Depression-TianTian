package grainalcohol.dtt.init;

import dev.architectury.registry.registries.DeferredRegister;
import grainalcohol.dtt.DTTMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

public class DTTStat {
    // NeoForge 在 mod 构造时原版注册表已冻结，必须走 DeferredRegister
    private static final DeferredRegister<ResourceLocation> CUSTOM_STATS = DeferredRegister.create(DTTMod.MOD_ID, Registries.CUSTOM_STAT);

    public static final ResourceLocation MONSTER_KILLED = DTTMod.id("monster_killed");
    public static final ResourceLocation PET_BRED = DTTMod.id("pet_bred");
    public static final ResourceLocation ANIMAL_TAMED = DTTMod.id("animal_tamed");
    public static final ResourceLocation RAID_LOSS = DTTMod.id("raid_loss");

    private static void register(ResourceLocation identifier, StatFormatter formatter) {
        // 统计项要等 id 真正注册进 CUSTOM_STAT 之后才能创建
        CUSTOM_STATS.register(identifier.getPath(), () -> identifier).listen(id -> Stats.CUSTOM.get(id, formatter));
    }

    public static void init() {
        register(MONSTER_KILLED, StatFormatter.DEFAULT);
        register(PET_BRED, StatFormatter.DEFAULT);
        register(ANIMAL_TAMED, StatFormatter.DEFAULT);
        register(RAID_LOSS, StatFormatter.DEFAULT);
        CUSTOM_STATS.register();
    }
}
