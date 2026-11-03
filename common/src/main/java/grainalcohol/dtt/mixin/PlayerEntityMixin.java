package grainalcohol.dtt.mixin;

import grainalcohol.dtt.config.DTTConfig;
import grainalcohol.dtt.diary.dailystat.v2.DailyStatManager;
import grainalcohol.dtt.init.DTTDailyStat;
import net.depression.mental.MentalStatus;
import net.depression.server.Registry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void onEatFoodReturn(Level world, ItemStack stack, FoodProperties foodProperties, CallbackInfoReturnable<ItemStack> cir) {
        Player self = (Player) (Object) this;
        if (self.level().isClientSide()) return;

        DailyStatManager.getTodayStat(self.getUUID()).setTrueStat(DTTDailyStat.ATE);
    }

    @Inject(
            method = "actuallyHurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void easierCombatStateAboutDamageTaken(DamageSource source, float amount, CallbackInfo ci) {
        // 未开启功能
        if (!DTTConfig.getInstance().getServerConfig().combatConfig.easierCombatState) return;

        Player self = (Player) (Object) this;
        MentalStatus mentalStatus = Registry.mentalStatus.get(self.getUUID());
        // depression这里写的是 mentalStatus.emotionValue <= 2.0
        if (mentalStatus.emotionValue > 2.0) {
            mentalStatus.combatCountdown = 10;
        }
    }
}
