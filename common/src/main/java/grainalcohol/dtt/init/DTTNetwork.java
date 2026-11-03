package grainalcohol.dtt.init;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import grainalcohol.dtt.DTTMod;
import grainalcohol.dtt.network.OpenEyesEventPacket;
import grainalcohol.dtt.network.ServerConfigPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class DTTNetwork {
    // 不再用 Architectury 的 NetworkChannel：它在客户端上把同一个 id 既登记成 C2S 又登记成 S2C，
    // 而 NeoForge 21.1 的 payload 注册表只按 id 判重、不分方向，第二次登记就会抛 "already registered"。
    // 所以每个方向单独一个 id，直接走 NetworkManager。
    public static final ResourceLocation OPEN_EYES_EVENT_C2S = DTTMod.id("open_eyes_event_c2s");
    public static final ResourceLocation SERVER_CONFIG_S2C = DTTMod.id("server_config_s2c");

    public static void init() {
        // 睁眼事件包（客户端 → 服务端）
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, OPEN_EYES_EVENT_C2S,
                (buf, context) -> new OpenEyesEventPacket(buf).apply(() -> context));
        // 客户端读取服务端配置选项包（服务端 → 客户端）：客户端登记接收，专用服务端只登记类型（Architectury 13 的约定）
        if (Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.Side.S2C, SERVER_CONFIG_S2C,
                    (buf, context) -> new ServerConfigPacket(buf).apply(() -> context));
        } else {
            NetworkManager.registerS2CPayloadType(SERVER_CONFIG_S2C);
        }
    }

    public static void sendToServer(OpenEyesEventPacket packet) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        packet.encode(buf);
        NetworkManager.sendToServer(OPEN_EYES_EVENT_C2S, buf);
    }

    public static void sendToPlayer(ServerPlayer player, ServerConfigPacket packet) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        packet.encode(buf);
        NetworkManager.sendToPlayer(player, SERVER_CONFIG_S2C, buf);
    }
}
