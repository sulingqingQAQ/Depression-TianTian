package grainalcohol.dtt.fabric;

import grainalcohol.dtt.DTTMod;
import net.fabricmc.api.ModInitializer;

public class DTTModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DTTMod.init();
    }
}
