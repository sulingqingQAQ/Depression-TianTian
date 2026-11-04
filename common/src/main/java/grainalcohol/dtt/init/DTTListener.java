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

public class DTTListener {
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
                    // 恶化到严重程度
                    DailyStatManager.getTodayStat(player.getUUID()).setTrueStat(DTTDailyStat.CURED);
                }
            }

            // 患病情况好转
            if (currentStatus.isHealthierThan(lastTickStatus)) {
                if (lastTickStatus.isSeverelyIll()) {
                    // 恢复到非严重程度
                    DailyStatManager.getTodayStat(player.getUUID()).setTrueStat(DTTDailyStat.WORSENED);
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

            HintMessageManager.onLogin(player);
        });
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
