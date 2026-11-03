package grainalcohol.dtt.neoforge;

import grainalcohol.dtt.DTTMod;
import grainalcohol.dtt.client.DTTModClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(DTTMod.MOD_ID)
public final class DTTModNeoForge {
    public DTTModNeoForge() {
        DTTMod.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            DTTModClient.init();
        }
    }
}
