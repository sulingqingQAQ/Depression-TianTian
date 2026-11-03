package grainalcohol.dtt.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import grainalcohol.dtt.DTTMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;

@Environment(EnvType.CLIENT)
public class DTTKeyBinds {
    public static final KeyMapping KEY = new KeyMapping(
            "key." + DTTMod.MOD_ID +".test",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_G,
            "category." + DTTMod.MOD_ID
    );

    public static void init() {
        KeyMappingRegistry.register(KEY);
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (KEY.isDown()) {
                // Toggle a custom shader effect
            }
        });
    }
}
