package grainalcohol.dtt.mixin;

import grainalcohol.dtt.diary.dailystat.v2.DailyStatManager;
import grainalcohol.dtt.init.DTTDailyStat;
import grainalcohol.dtt.init.DTTStat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

@Mixin(Animal.class)
public class AnimalEntityMixin {
    @Inject(
            method = "finalizeSpawnChildFromBreeding(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/Animal;Lnet/minecraft/world/entity/AgeableMob;)V",
            at = @At("HEAD")
    )
    public void breed(ServerLevel world, Animal other, AgeableMob baby, CallbackInfo ci) {
        Animal self = (Animal) (Object) this;
        Optional.ofNullable(self.getLoveCause()).or(
                () -> Optional.ofNullable(other.getLoveCause())
                // 从父母中尝试获取玩家信息
        ).ifPresent(
                (player) -> {
                    // 繁殖宠物
                    player.awardStat(DTTStat.PET_BRED);
                    DailyStatManager.getTodayStat(player.getUUID()).setTrueStat(DTTDailyStat.PET_BRED);
                }
        );
    }
}
