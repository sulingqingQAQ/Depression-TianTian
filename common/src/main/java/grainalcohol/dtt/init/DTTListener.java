package grainalcohol.dtt.init;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import grainalcohol.dtt.api.event.MentalIllnessEvent;
import grainalcohol.dtt.api.event.PTSDEvent;
import grainalcohol.dtt.api.event.SymptomEvent;
import grainalcohol.dtt.api.helper.MentalStatusHelper;
import grainalcohol.dtt.api.internal.EyesStatusFlagController;
import grainalcohol.dtt.config.DTTConfig;
import grainalcohol.dtt.config.ServerConfig;
import grainalcohol.dtt.diary.dailystat.v2.DailyStatManager;
import grainalcohol.dtt.api.wrapper.PTSDLevel;
import grainalcohol.dtt.hint.HintMessageManager;
import grainalcohol.dtt.network.ServerConfigPacket;
import dev.architectury.event.events.common.PlayerEvent;
import net.depression.effect.EffectHolders;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DTTListener {
    /**
     * 已退出、等待"退出存档写完之后"再清理内存数据的玩家。<br>
     * 不能在 PLAYER_QUIT 里直接清：Architectury 的 PLAYER_QUIT 在 {@code PlayerList.remove} 头部触发
     * （fabric: MixinPlayerList@HEAD；neoforge: {@code EventHooks.firePlayerLoggedOut}），
     * 而原版紧接着才调 {@code save(player)} → {@code addAdditionalSaveData}。此时玩家数据还没进 NBT，
     * 提前清表会把这次的日记/统计/提示直接写丢。<br>
     * 所以这里只登记 UUID，真正清理放在 {@code ServerPlayerEntityMixin.onWriteCustomDataToNbt} 的 TAIL。
     */
    private static final Set<UUID> pendingLogoutCleanup = ConcurrentHashMap.newKeySet();

    /** 退出时只登记，不做清理（写入 NBT 之前清会丢数据）。 */
    public static void markLogoutCleanup(UUID playerId) {
        pendingLogoutCleanup.add(playerId);
    }

    /**
     * 取出并移除退出登记。<br>
     * 返回 true 表示"这次写 NBT 就是退出存档，写完可以清理该玩家的内存数据"。
     * 用 remove 保证只清一次；正常在线自动存档从未登记，永远返回 false。
     */
    public static boolean consumeLogoutCleanup(UUID playerId) {
        return pendingLogoutCleanup.remove(playerId);
    }

    public static void archEventInit() {
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            // 宠物死亡
            if (entity instanceof TamableAnimal tameable
                    && tameable.isTame()
                    && tameable.getOwner() instanceof ServerPlayer owner
            ) {
                DailyStatManager.getTodayStat(owner.getUUID()).setTrueStat(DTTDailyStat.PET_DIED);
            }
            // 击杀怪物
            if (entity instanceof Enemy
                    && source.getEntity() instanceof ServerPlayer player
            ) {
                player.awardStat(DTTStat.MONSTER_KILLED);
                DailyStatManager.getTodayStat(player.getUUID()).incrementNumberStat(DTTDailyStat.MONSTER_KILLED);
            }
            // 玩家死亡
            if (entity instanceof Player player) {
                DailyStatManager.getTodayStat(player.getUUID()).setTrueStat(DTTDailyStat.DIED);
            }
            return EventResult.pass();
        });
        EntityEvent.ANIMAL_TAME.register(((animalEntity, playerEntity) -> {
            playerEntity.awardStat(DTTStat.ANIMAL_TAMED);
            return EventResult.pass();
        }));
    }
    public static void dttEventInit() {
        MentalIllnessEvent.MENTAL_HEALTH_CHANGED_EVENT.register((player, lastTickStatus, currentStatus) -> {
            // 患病情况恶化
            if (currentStatus.isSickerThan(lastTickStatus)) {
                if (currentStatus.isSeverelyIll()) {
                    // 恶化到严重程度（CURED/WORSENED 原本写反，导致日记里"康复"和"恶化"两段互换）
                    DailyStatManager.getTodayStat(player.getUUID()).setTrueStat(DTTDailyStat.WORSENED);
                }
            }

            // 患病情况好转
            if (currentStatus.isHealthierThan(lastTickStatus)) {
                if (lastTickStatus.isSeverelyIll()) {
                    // 恢复到非严重程度
                    DailyStatManager.getTodayStat(player.getUUID()).setTrueStat(DTTDailyStat.CURED);
                }

                if (currentStatus.isHealthy() && player.hasEffect(EffectHolders.resolve(DTTStatusEffect.ANOREXIA))) {
                    // 恢复到健康状态时移除厌食状态效果
                    player.removeEffect(EffectHolders.resolve(DTTStatusEffect.ANOREXIA));
                }
            }
        });
        PTSDEvent.PTSD_LEVEL_CHANGED_EVENT.register(((player, context, lastLevel, currentLevel) -> {
            if (currentLevel.isSickerThan(lastLevel)) {
                // PTSD等级提升
                onPTSDLevelUp(player, context.getPtsdInfo(), currentLevel);
            }
        }));
        PTSDEvent.PTSD_DISPERSE_EVENT.register((player, context) -> {
            onPTSDDisperse(player, context.getPtsdInfo());
        });
        PTSDEvent.PTSD_REMISSION_EVENT.register((player, context, currentLevel) -> {
            onPTSDRemission(player, context.getPtsdInfo());
        });
        SymptomEvent.CLOSE_EYES_EVENT.register((player, causedBySleepinessStatusEffect) -> {
            if (!causedBySleepinessStatusEffect &&
                    MentalStatusHelper.isInCatatonicStuporStatus(player) &&
                    DTTConfig.getInstance().getServerConfig().combatConfig.saferCatatonicStupor
            ) {
                // 缓慢 + 挖掘疲劳 + 虚弱
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        30, 4
                ));
                player.addEffect(new MobEffectInstance(
                        MobEffects.DIG_SLOWDOWN,
                        30, 4
                ));
                player.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS,
                        30, 4
                ));
            }
            return EventResult.pass();
        });
    }

    public static void dttInternalEventInit() {
        SymptomEvent.CLOSE_EYES_EVENT.register((player, causedBySleepinessStatusEffect) -> {
            if (player instanceof EyesStatusFlagController controller) {
                controller.dtt$setIsEyesClosedFlag(true);
            }
            return EventResult.pass();
        });
        SymptomEvent.OPEN_EYES_EVENT.register(player -> {
            if (player instanceof EyesStatusFlagController controller) {
                controller.dtt$setIsEyesClosedFlag(false);
            }
        });
    }
    public static void connectionEventInit() {
        PlayerEvent.PLAYER_JOIN.register(player -> {
            ServerConfig serverConfig = DTTConfig.getInstance().getServerConfig();

            DTTNetwork.sendToPlayer(player, new ServerConfigPacket(
                    serverConfig.combatConfig.saferCatatonicStupor,
                    serverConfig.commonConfig.disableMentalTraitSelectScreen
            ));

            // 丢弃上一次异常退出（存档中途抛错、没走到清理）可能残留的登记，否则重进后第一次在线自动存档会误清这名玩家的数据
            consumeLogoutCleanup(player.getUUID());
            HintMessageManager.onLogin(player);
        });
        // 退出时只登记，真正的清理等 addAdditionalSaveData 把数据全写完之后再做
        PlayerEvent.PLAYER_QUIT.register(player -> markLogoutCleanup(player.getUUID()));
    }

    public static void onPTSDLevelUp(ServerPlayer player, String ptsdInfo, PTSDLevel currentLevel) {
        if (!DTTConfig.getInstance().getClientConfig().messageDisplayConfig.enhancedPTSDFormationMessage) {
            return;
        }
        // PTSD等级提升至0级
        if (currentLevel.isLatent()) {
            DTTHintMessage.LATENT_PTSD_MESSAGE.trigger(player, ptsdInfo);
        }
        // PTSD等级提升至1、2、3级
        if (currentLevel.hasSymptoms()) {
            DTTHintMessage.FORM_PTSD_MESSAGE.trigger(player, ptsdInfo);
        }
        // PTSD等级提升至4级
        if (currentLevel.isExtreme()) {
            DTTHintMessage.EXTREME_PTSD_MESSAGE.trigger(player, ptsdInfo);
        }
    }

    private static void onPTSDDisperse(ServerPlayer player, String ptsdInfo) {
        if (!DTTConfig.getInstance().getClientConfig().messageDisplayConfig.enhancedPTSDDispersalMessage) {
            return;
        }
        DTTHintMessage.DISPERSE_PTSD_MESSAGE.trigger(player, ptsdInfo);
    }

    private static void onPTSDRemission(ServerPlayer player, String ptsdInfo) {
        if (!DTTConfig.getInstance().getClientConfig().messageDisplayConfig.enhancedPTSDRemissionMessage) {
            return;
        }
        DTTHintMessage.REMISSION_PTSD_MESSAGE.trigger(player, ptsdInfo);
    }
}
