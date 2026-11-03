package grainalcohol.dtt.mixin;

import grainalcohol.dtt.api.internal.EyesStatusFlagController;
import grainalcohol.dtt.config.DTTConfig;
import grainalcohol.dtt.config.ServerConfig;
import grainalcohol.dtt.diary.dailystat.v2.DailyStatManager;
import grainalcohol.dtt.diary.topic.v2.TopicManager;
import grainalcohol.dtt.hint.HintMessageManager;
import grainalcohol.dtt.init.DTTDailyStat;
import grainalcohol.dtt.init.DTTHintMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin implements EyesStatusFlagController {
    // 服务端标记
    @Unique private boolean dtt$isEyesClosed = false;

    @Override
    public boolean dtt$getIsEyesClosedFlag() {
        return this.dtt$isEyesClosed;
    }

    @Override
    public void dtt$setIsEyesClosedFlag(boolean isClosed) {
        this.dtt$isEyesClosed = isClosed;
    }

    @Inject(
            method = "setRespawnPosition",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;sendSystemMessage(Lnet/minecraft/network/chat/Component;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onSetSpawnPoint(ResourceKey<Level> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage, CallbackInfo ci) {
        if (!sendMessage) {
            return;
        }

        ServerPlayer self = (ServerPlayer) (Object) this;
        ServerLevel serverWorld = self.serverLevel();

        // TODO: 把延迟功能做到HintMessage里
        new Thread(() -> {
            try {
                // 5s later
                Thread.sleep(5000);

                serverWorld.getServer().execute(() ->
                        DTTHintMessage.RESET_SPAWN_POINT_MESSAGE.trigger(self)
                );
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        ServerLevel serverWorld = self.serverLevel();
        ServerConfig serverConfig = DTTConfig.getInstance().getServerConfig();
        if (serverWorld.getDayTime() % 24000 == 0) {
            // 每天0时更新统计数据
            double EMA_Factor = serverConfig.diaryConfig.EMAFactor;
            DailyStatManager.updateDailyStat(self.getUUID(), EMA_Factor);
        }
        HintMessageManager.tick(self);
    }

    @Inject(
            method = "awardStat(Lnet/minecraft/stats/Stat;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/stats/ServerStatsCounter;increment(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/stats/Stat;I)V",
                    shift = At.Shift.AFTER
            )
    )
    private void increaseDailyStat(Stat<?> stat, int amount, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (stat.getType() == Stats.ENTITY_KILLED && stat.getValue().equals(EntityType.ENDER_DRAGON)) {
            DailyStatManager.getTodayStat(self.getUUID()).setTrueStat(DTTDailyStat.ENDER_DRAGON_KILLED);
        }

        if (stat.getType() == Stats.CUSTOM) {
            ResourceLocation statId = (ResourceLocation) stat.getValue();

            if (statId.equals(Stats.WALK_ONE_CM)) {
                DailyStatManager.getTodayStat(self.getUUID()).increaseNumberStat(DTTDailyStat.DISTANCE_MOVED, amount);
            }
            if (statId.equals(Stats.TRADED_WITH_VILLAGER)) {
                DailyStatManager.getTodayStat(self.getUUID()).increaseNumberStat(DTTDailyStat.TRADED_COUNT, amount);
            }
            if (statId.equals(Stats.POT_FLOWER)) {
                DailyStatManager.getTodayStat(self.getUUID()).setTrueStat(DTTDailyStat.FLOWER_POTTED);
            }
            if (statId.equals(Stats.RAID_WIN)) {
                DailyStatManager.getTodayStat(self.getUUID()).setTrueStat(DTTDailyStat.RAID_WON);
            }
            if (statId.equals(Stats.DAMAGE_TAKEN)) {
                DailyStatManager.getTodayStat(self.getUUID()).increaseNumberStat(DTTDailyStat.DAMAGE_TAKEN, amount);
            }
        }
    }

    @Inject(
            method = "triggerDimensionChangeTriggers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/advancements/critereon/ChangeDimensionTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/resources/ResourceKey;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onWorldChanged(ServerLevel origin, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        ServerLevel newWorld = self.serverLevel();
        if (newWorld.dimensionType().hasSkyLight() && newWorld.isRaining()) {
            // 切换维度时
            DailyStatManager.getTodayStat(self.getUUID()).setTrueStat(DTTDailyStat.RAINED);
        }
    }

    @Inject(method = "stopSleepInBed", at = @At("TAIL"))
    private void onWakeUp(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        ServerLevel serverWorld = self.serverLevel();
        if (!(self.getSleepTimer() == 0)) {
            // 不是自然醒
            return;
        }

        if (serverWorld.dimensionType().hasSkyLight() && serverWorld.isRaining()) {
            // 睡醒时
            DailyStatManager.getTodayStat(self.getUUID()).setTrueStat(DTTDailyStat.RAINED);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void onWriteCustomDataToNbt(CompoundTag nbt, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        nbt.putBoolean("dtt$isEyesClosed", this.dtt$isEyesClosed);

        DailyStatManager.writeToNbt(self.getUUID(), nbt);

        TopicManager.writeToNbt(self.getUUID(), nbt);

        HintMessageManager.writeToNbt(self.getUUID(), nbt);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void onReadCustomDataFromNbt(CompoundTag nbt, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        this.dtt$isEyesClosed = nbt.getBoolean("dtt$isEyesClosed");

        DailyStatManager.readFromNbt(self.getUUID(), nbt);

        TopicManager.readFromNbt(self.getUUID(), nbt);

        HintMessageManager.readFromNbt(self.getUUID(), nbt);
    }
}
