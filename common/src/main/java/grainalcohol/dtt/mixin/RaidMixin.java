package grainalcohol.dtt.mixin;

import grainalcohol.dtt.diary.dailystat.v2.DailyStatManager;
import grainalcohol.dtt.init.DTTDailyStat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;

@Mixin(Raid.class)
public abstract class RaidMixin {
    @Unique private boolean dtt$raidHasLostYet = false;

    @Shadow protected abstract Predicate<ServerPlayer> validPlayer();

    @Inject(method = "tick", at = @At("TAIL"))
    public void onTick(CallbackInfo ci) {
        Raid raid = (Raid) (Object) this;
        if (!(raid.getLevel() instanceof ServerLevel serverWorld)) {
            return;
        }

        if (!raid.isLoss()) {
            dtt$raidHasLostYet = false;
            return;
        }

        if (!dtt$raidHasLostYet && raid.isLoss()) {
            dtt$raidHasLostYet = true;
            List<ServerPlayer> players = serverWorld.getPlayers(validPlayer());
            players.forEach(player -> DailyStatManager.getTodayStat(player.getUUID())
                    .setTrueStat(DTTDailyStat.RAID_FAILED)
            );
        }
    }
}
