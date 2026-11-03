package grainalcohol.dtt.fabric;

import grainalcohol.dtt.client.DTTModClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class DTTModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DTTModClient.init();
    }
}
