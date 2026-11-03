package grainalcohol.dtt.fabric.mixin;

import grainalcohol.dtt.util.PsychologistTradeOffers;
import net.depression.fabric.world.FabricVillageAdditions;
import net.depression.world.VillageAdditions;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FabricVillageAdditions.class, remap = false)
public class VillageAdditionsMixin {
    @Inject(
            method = "register",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/depression/fabric/world/FabricVillageAdditions;registerPoi(Lnet/minecraft/resources/ResourceKey;Ldev/architectury/registry/registries/RegistrySupplier;)V",
                    shift = At.Shift.AFTER,
                    remap = true
            ),
            cancellable = true
    )
    private static void afterRegisterPoi(CallbackInfo ci) {
        PsychologistTradeOffers.register((lowestLevel, listing) -> TradeOfferHelper.registerVillagerOffers(
                VillageAdditions.PSYCHOLOGIST.get(), lowestLevel,
                factories -> factories.add(listing)
        ));

        ci.cancel();
    }
}
