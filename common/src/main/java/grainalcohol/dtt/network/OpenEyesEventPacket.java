package grainalcohol.dtt.network;

import dev.architectury.networking.NetworkManager;
import grainalcohol.dtt.api.event.SymptomEvent;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class OpenEyesEventPacket {
    public OpenEyesEventPacket() {}

    // 编码
    public void encode(FriendlyByteBuf buf) {}

    // 解码
    public OpenEyesEventPacket(FriendlyByteBuf buf) {}

    // 应用
    public void apply(Supplier<NetworkManager.PacketContext> context) {
        if (context.get().getPlayer() instanceof ServerPlayer serverPlayerEntity) {
            context.get().queue(
                    () -> SymptomEvent.OPEN_EYES_EVENT.invoker().onOpenEyes(serverPlayerEntity)
            );
        }
    }
}
