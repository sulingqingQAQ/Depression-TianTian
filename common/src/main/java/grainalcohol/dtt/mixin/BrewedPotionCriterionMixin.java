package grainalcohol.dtt.mixin;

import grainalcohol.dtt.diary.dailystat.v2.DailyStatManager;
import grainalcohol.dtt.init.DTTDailyStat;
import net.minecraft.advancements.critereon.BrewedPotionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewedPotionTrigger.class)
public class BrewedPotionCriterionMixin {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void onTriggerInject(ServerPlayer player, Holder<Potion> potion, CallbackInfo ci) {
        // 1.21 起药水是 Holder，且没有 Potions.EMPTY 了（空药水不再是一种 Potion）
        if (player == null || potion == null || potion.value().getEffects().isEmpty() || potion.is(Potions.WATER)) {
            return;
        }
        DailyStatManager.getTodayStat(player.getUUID()).incrementNumberStat(DTTDailyStat.POTION_BREWED);
    }
}
