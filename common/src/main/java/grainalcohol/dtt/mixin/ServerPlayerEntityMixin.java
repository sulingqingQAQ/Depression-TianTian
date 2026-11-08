package grainalcohol.dtt.mixin;

import grainalcohol.dtt.api.internal.EyesStatusFlagController;
import grainalcohol.dtt.api.internal.SpawnResetHintController;
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
public abstract class ServerPlayerEntityMixin implements EyesStatusFlagController, SpawnResetHintController {
    // 服务端标记
    @Unique private boolean dtt$isEyesClosed = false;
    // 上一次见过的"天序号"（dayTime/24000）；Long.MIN_VALUE 表示本次上线还没初始化
    @Unique private long dtt$lastDayIndex = Long.MIN_VALUE;
    // "重置重生点"提示的到期游戏刻；Long.MIN_VALUE 表示当前没有待触发的提示（替代原来的 sleep 线程）
    @Unique private long dtt$spawnResetHintAt = Long.MIN_VALUE;

    @Override
    public boolean dtt$getIsEyesClosedFlag() {
        return this.dtt$isEyesClosed;
    }

    @Override
    public void dtt$setIsEyesClosedFlag(boolean isClosed) {
        this.dtt$isEyesClosed = isClosed;
    }

    /**
     * 取消尚未到期的"重置重生点"提示。<br>
     * 由 {@code PlayerList.remove} 的 HEAD 调用（见 {@code grainalcohol.dtt.mixin.PlayerListMixin}）：
     * 登出 / 关服时这条瞬态提示一律作废，不再像旧实现那样靠写 NBT 的副作用顺手清。
     */
    @Override
    public void dtt$cancelSpawnResetHint() {
        this.dtt$spawnResetHintAt = Long.MIN_VALUE;
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
        // 原来这里是 new Thread + Thread.sleep(5000)：5 秒内重复设置会线性堆线程，且线程会持有旧 ServerPlayer/ServerLevel。
        // 现在只登记到期游戏刻（5 秒 = 100 tick），由本类已有的 tick 注入倒计时触发；重复设置直接覆盖，合并成一次提示。
        this.dtt$spawnResetHintAt = self.serverLevel().getGameTime() + 100L;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        ServerLevel serverWorld = self.serverLevel();
        ServerConfig serverConfig = DTTConfig.getInstance().getServerConfig();
        // 日切判定改用"天序号变化"：dayTime/24000 变化一次才更新一次统计，天然幂等。
        // 原来的 dayTime%24000==0 在 doDaylightCycle=false 把时间冻结在 24000 整数倍时会每 tick 成立，
        // 于是每 tick 都把当天统计清空（醒着的玩家永远攒不到数据）
        long dayIndex = serverWorld.getDayTime() / 24000L;
        if (dayIndex != this.dtt$lastDayIndex) {
            // 上线后的第一 tick 只记录当前天序号，不触发日切，避免刚登录就把今天的统计滚到昨天
            boolean initialized = this.dtt$lastDayIndex != Long.MIN_VALUE;
            this.dtt$lastDayIndex = dayIndex;
            if (initialized) {
                // 每天0时更新统计数据
                double EMA_Factor = serverConfig.diaryConfig.EMAFactor;
                DailyStatManager.updateDailyStat(self.getUUID(), EMA_Factor);
            }
        }
        // 重置重生点提示的倒计时：到期先清标记再触发（触发时抛异常也不会重复发），断线/关服后不补发
        if (this.dtt$spawnResetHintAt != Long.MIN_VALUE && serverWorld.getGameTime() >= this.dtt$spawnResetHintAt) {
            this.dtt$spawnResetHintAt = Long.MIN_VALUE;
            if (!self.hasDisconnected() && self.server.isRunning()) {
                DTTHintMessage.RESET_SPAWN_POINT_MESSAGE.trigger(self);
            }
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

        // 序列化是纯读：这里只把内存状态投影进 NBT，不新建对象、不清理、不消费任何标记。
        // 清理挪到 PlayerList.remove 的 RETURN（见 DTTPlayerState），登录残留由 placeNewPlayer HEAD 兜住。
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
