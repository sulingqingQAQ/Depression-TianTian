package grainalcohol.dtt.network;

import dev.architectury.networking.NetworkManager;
import grainalcohol.dtt.client.DTTServerConfigCache;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;

public class ServerConfigPacket {
    public boolean saferCatatonicStupor;
    public boolean disableMentalTraitSelectScreen;

    public ServerConfigPacket(boolean saferCatatonicStupor, boolean disableMentalTraitSelectScreen) {
        this.saferCatatonicStupor = saferCatatonicStupor;
        this.disableMentalTraitSelectScreen = disableMentalTraitSelectScreen;
    }

    // 编码
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(saferCatatonicStupor);
        buf.writeBoolean(disableMentalTraitSelectScreen);
    }

    // 解码
    public ServerConfigPacket(FriendlyByteBuf buf) {
        this.saferCatatonicStupor = buf.readBoolean();
        this.disableMentalTraitSelectScreen = buf.readBoolean();
    }

    // 应用
    public void apply(Supplier<NetworkManager.PacketContext> context) {
        context.get().queue(() -> {
            DTTServerConfigCache.saferCatatonicStupor = this.saferCatatonicStupor;
            DTTServerConfigCache.disableMentalTraitSelectScreen = this.disableMentalTraitSelectScreen;
        });
    }
}
