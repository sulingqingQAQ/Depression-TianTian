package grainalcohol.dtt.mixin.modification;

import dev.architectury.networking.NetworkManager;
import grainalcohol.dtt.config.DTTConfig;
import grainalcohol.dtt.diary.DiaryContentProducer;
import io.netty.buffer.Unpooled;
import net.depression.mental.MentalStatus;
import net.depression.network.DiaryUpdatePacket;
import net.depression.server.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DiaryUpdatePacket.class, remap = false)
public class DiaryUpdatePacketMixin {
    @Inject(method = "sendToPlayer", at = @At("HEAD"), cancellable = true)
    private static void sendToPlayer(ServerPlayer player, CallbackInfo ci) {
        if (!DTTConfig.getInstance().getServerConfig().diaryConfig.enhancedDiaryGenerator) {
            return;
        }

        MentalStatus mentalStatus = Registry.mentalStatus.get(player.getUUID());
        if (mentalStatus == null) {
            Registry.mentalStatus.put(player.getUUID(), new MentalStatus(player));
        }

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        DiaryContentProducer diaryContentProducer = new DiaryContentProducer(player);

        buf.writeCharSequence(diaryContentProducer.completeTranslationKeyProcess(), DiaryUpdatePacket.charset);
        NetworkManager.sendToPlayer(player, DiaryUpdatePacket.DIARY_UPDATE_PACKET, buf);

        ci.cancel();
    }
}