package grainalcohol.dtt.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import grainalcohol.dtt.diary.dailystat.v2.DailyStatManager;
import grainalcohol.dtt.init.DTTDailyStat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public abstract class ServerWorldMixin {
    @WrapOperation(
            method = "advanceWeatherCycle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/ServerLevelData;setRaining(Z)V"
            )
    )
    private void onSetRaining(ServerLevelData properties, boolean isRaining, Operation<Void> original) {
        if (!properties.isRaining() && isRaining || properties.isRaining() && !isRaining) {
            // 开始或停止下雨
            ServerLevel serverWorld = (ServerLevel) (Object) this;
            updateHasRained(serverWorld);
        }
        original.call(properties, isRaining);
    }

    @WrapOperation(
            method = "advanceWeatherCycle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/ServerLevelData;setThundering(Z)V"
            )
    )
    private void onSetThundering(ServerLevelData properties, boolean isThundering, Operation<Void> original) {
        if (properties.isThundering() != isThundering) {
            // 开始或停止雷暴
            ServerLevel serverWorld = (ServerLevel) (Object) this;
            updateHasRained(serverWorld);
        }
        original.call(properties, isThundering);
    }

    @Unique
    private static void updateHasRained(ServerLevel serverWorld) {
        if (18000 < serverWorld.getDayTime() && serverWorld.getDayTime() < 22000) {
            // 0点后到日出前的变化不记录
            return;
        }
        for (ServerPlayer player : serverWorld.getPlayers(player -> player.serverLevel().dimensionType().hasSkyLight())) {
            DailyStatManager.getTodayStat(player.getUUID()).setTrueStat(DTTDailyStat.RAINED);
        }
    }
}
