package grainalcohol.dtt.mixin.modification;

import com.llamalad7.mixinextras.sugar.Local;
import dev.architectury.event.EventResult;
import grainalcohol.dtt.config.DTTConfig;
import net.depression.listener.PlayerEventListener;
import net.depression.mental.MentalStatus;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerEventListener.class, remap = false)
public class PlayerEventListenerMixin {
    @Inject(
            method = "onAttackEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/depression/mental/MentalIllness;trigMentalFatigue()V"
            )
    )
    private static void beforeTrigMentalFatigue(Player player, Level level, Entity entity, InteractionHand hand, EntityHitResult entityHitResult, CallbackInfoReturnable<EventResult> cir, @Local(name = "mentalStatus")MentalStatus mentalStatus) {
        if (!DTTConfig.getInstance().getServerConfig().combatConfig.easierCombatState) {
            return;
        }

        if (mentalStatus.emotionValue < -2.0) {
            mentalStatus.combatCountdown = 10;
        }
    }
}
