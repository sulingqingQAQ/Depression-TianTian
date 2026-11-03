package grainalcohol.dtt.neoforge.mixin;

import grainalcohol.dtt.util.PsychologistTradeOffers;
import net.depression.neoforge.world.NeoForgeVillageAddition;
import net.depression.world.VillageAdditions;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

// 对应 Fabric 端的 VillageAdditionsMixin：用 DTT 配置的交易表替换 Depression 自带的心理医生交易
@Mixin(value = NeoForgeVillageAddition.class, remap = false)
public class NeoForgeVillageAdditionMixin {
    @Inject(method = "registerVillagerTrades", at = @At("HEAD"), cancellable = true)
    private static void replacePsychologistTrades(VillagerTradesEvent event, CallbackInfo ci) {
        if (event.getType() != VillageAdditions.PSYCHOLOGIST.get()) {
            return;
        }
        PsychologistTradeOffers.register((lowestLevel, listing) ->
                event.getTrades().computeIfAbsent(lowestLevel, level -> new ArrayList<>()).add(listing));

        ci.cancel();
    }
}
