package grainalcohol.dtt.config;

import com.google.gson.annotations.SerializedName;
import grainalcohol.dtt.diary.DiaryContentProducer;
import grainalcohol.dtt.diary.DiaryParagraph;
import grainalcohol.dtt.diary.feeling.FeelingProducer;
import grainalcohol.dtt.diary.topic.TopicWeightCalculator;
import grainalcohol.dtt.mixin.PlayerEntityMixin;
import grainalcohol.dtt.mixin.modification.ClientTickEventListenerMixin;
import grainalcohol.dtt.mixin.modification.MentalHealthScaleItemMixin;
import grainalcohol.dtt.mixin.modification.PlayerEventListenerMixin;
import grainalcohol.dtt.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class ServerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfig.class);

    @SerializedName("villager_config")
    public VillagerConfig villagerConfig = new VillagerConfig();
    @SerializedName("diary_config")
    public DiaryConfig diaryConfig = new DiaryConfig();
    @SerializedName("combat_config")
    public CombatConfig combatConfig = new CombatConfig();
    @SerializedName("ptsd_config")
    public PTSDConfig PTSDConfig = new PTSDConfig();
    @SerializedName("common_config")
    public CommonConfig commonConfig = new CommonConfig();
    @SerializedName("item_config")
    public ItemConfig itemConfig = new ItemConfig();
    @SerializedName("mental_heal_config")
    public MentalHealConfig mentalHealConfig = new MentalHealConfig();

    /**
     * 加载后的分组与数值校验，由 {@link DTTConfig#loadConfig} 在 Gson 反序列化成功后调用。<br>
     * 两种输入会让配置"看起来加载成功、实际不可用"，这里统一兜住并报错：
     * <ul>
     *     <li>JSON 里写了 {@code "combat_config": null} 之类的分组 → Gson 把该分组字段覆盖成 null，
     *     后续 {@code getServerConfig().combatConfig.saferCatatonicStupor}（玩家加入、玩家 tick）直接 NPE；</li>
     *     <li>{@code ema_factor} 越界（含 NaN/Infinity）→ {@code DailyStatManager.updateDailyStat} 里
     *     {@code previousEMA.multiply(1 - EMAFactor)} 会给出负贡献，权重随之失真。</li>
     * </ul>
     * 合法配置（包括缺少某个键、由字段初始值补全的旧配置）不会被改动。
     *
     * @param fileName 配置文件名，仅用于错误日志定位
     */
    public void validate(String fileName) {
        if (villagerConfig == null) {
            LOGGER.error("Config {}: field 'villager_config' is missing or null, falling back to default group", fileName);
            villagerConfig = new VillagerConfig();
        }
        if (diaryConfig == null) {
            LOGGER.error("Config {}: field 'diary_config' is missing or null, falling back to default group", fileName);
            diaryConfig = new DiaryConfig();
        }
        if (combatConfig == null) {
            LOGGER.error("Config {}: field 'combat_config' is missing or null, falling back to default group", fileName);
            combatConfig = new CombatConfig();
        }
        if (PTSDConfig == null) {
            LOGGER.error("Config {}: field 'ptsd_config' is missing or null, falling back to default group", fileName);
            PTSDConfig = new PTSDConfig();
        }
        if (commonConfig == null) {
            LOGGER.error("Config {}: field 'common_config' is missing or null, falling back to default group", fileName);
            commonConfig = new CommonConfig();
        }
        if (itemConfig == null) {
            LOGGER.error("Config {}: field 'item_config' is missing or null, falling back to default group", fileName);
            itemConfig = new ItemConfig();
        }
        if (mentalHealConfig == null) {
            LOGGER.error("Config {}: field 'mental_heal_config' is missing or null, falling back to default group", fileName);
            mentalHealConfig = new MentalHealConfig();
        }

        // EMA 因子必须落在 [0,1]：NaN 与 ±Infinity 也走这里（!(x>=0 && x<=1) 对 NaN 为真）
        double emaFactor = diaryConfig.EMAFactor;
        if (!(emaFactor >= 0d && emaFactor <= 1d)) {
            double fixed = Double.isNaN(emaFactor) ? 0.5d : Math.max(0d, Math.min(1d, emaFactor));
            LOGGER.warn("Config {}: 'diary_config.ema_factor' = {} is out of range [0, 1], clamped to {}", fileName, emaFactor, fixed);
            diaryConfig.EMAFactor = fixed;
        }

        // 变体数量上限与 StringUtil 的硬上限保持一致，避免配置写错后变体查找循环失控
        int variantCount = diaryConfig.diaryTranslationKeyVariantCount;
        if (variantCount < 0 || variantCount > StringUtil.MAX_VARIANT_COUNT) {
            int fixed = Math.max(0, Math.min(StringUtil.MAX_VARIANT_COUNT, variantCount));
            LOGGER.warn("Config {}: 'diary_config.diary_translation_key_variant_count' = {} is out of range [0, {}], clamped to {}",
                    fileName, variantCount, StringUtil.MAX_VARIANT_COUNT, fixed);
            diaryConfig.diaryTranslationKeyVariantCount = fixed;
        }
    }

    public static class MentalHealConfig {
        /**
         * depression原版为20，默认600，单位为tick，附近方块提供情绪值恢复的间隔时间
         */
        @SerializedName("nearby_block_interval_ticks")
        public int nearbyBlockIntervalTicks = 600;
        /**
         * depression原版为everyone，默认max_only，决定附近那些方块能提供情绪值恢复<br>
         * 可选值：
         * <ul>
         *     <li>everyone：应用附近所有方块的总恢复量</li>
         *     <li>max_only：仅应用回复量最大的那一个</li>
         *     <li>nothing：不回复</li>
         * </ul>
         */
        @SerializedName("nearby_block_mode")
        public NearbyMultipleBlocksHealMode nearbyBlockMode = NearbyMultipleBlocksHealMode.MAX_ONLY;

        /**
         * depression原版为20，默认600，单位为tick，宠物恢复情绪值的间隔时间，仅nearbyPetMode为EVERYONE时有效
         */
        @SerializedName("nearby_pet_interval_ticks")
        public int nearbyPetIntervalTicks = 600;
        /**
         * depression原版为everyone，默认exist，决定附近那些宠物能提供情绪值恢复<br>
         * 可选值：
         * <ul>
         *     <li>everyone：应用附近所有宠物的总恢复量</li>
         *     <li>exist：只要附近存在宠物就提供固定的恢复</li>
         *     <li>nothing：不回复</li>
         * </ul>
         */
        @SerializedName("nearby_pet_mode")
        public NearbyAnythingHealMode nearbyPetMode = NearbyAnythingHealMode.EXIST;

        /**
         * depression原版为20，默认600，单位为tick，唱片机恢复情绪的间隔时间，仅nearbyJukeboxMode为EVERYONE时有效
         */
        @SerializedName("nearby_jukebox_interval_ticks")
        public int nearbyJukeboxIntervalTicks = 600;
        /**
         * depression原版为everyone，默认exist，决定附近那些唱片机能提供情绪值恢复<br>
         * 可选值：
         * <ul>
         *     <li>everyone：应用附近所有唱片机的总恢复量</li>
         *     <li>exist：只要附近存在唱片机就提供固定的恢复</li>
         *     <li>nothing：不回复</li>
         * </ul>
         */
        @SerializedName("nearby_jukebox_mode")
        public NearbyAnythingHealMode nearbyJukeboxMode = NearbyAnythingHealMode.EXIST;
    }

    /**
     * 村民相关配置
     * @see grainalcohol.dtt.util.PsychologistTradeOffers
     */
    public static class VillagerConfig {
        /**
         * 默认false，启用后心理医生（下简称村民）的商品价格会随供需关系浮动
         */
        @SerializedName("enable_price_floating")
        public boolean enablePriceFloating = false;

        /**
         * 默认1，村民售卖精神健康量表的所需等级
         */
        @SerializedName("lowest_level_of_mental_health_scale")
        public int lowestLevelOfMentalHealthScale = 1;
        /**
         * 默认6，精神健康量表的基础价格
         */
        @SerializedName("base_price_of_mental_health_scale")
        public int basePriceOfMentalHealthScale = 6;
        /**
         * 默认8，精神健康量表的最大库存量
         */
        @SerializedName("max_uses_of_mental_health_scale")
        public int maxUsesOfMentalHealthScale = 8;

        /**
         * 默认1，村民售卖轻度抗抑郁药片的所需等级
         */
        @SerializedName("lowest_level_of_mild_depression_tablet")
        public int lowestLevelOfMildDepressionTablet = 1;
        /**
         * 默认20，轻度抗抑郁药片的基础价格
         */
        @SerializedName("base_price_of_mild_depression_tablet")
        public int basePriceOfMildDepressionTablet = 20;
        /**
         * 默认10，轻度抗抑郁药片的最大库存量
         */
        @SerializedName("max_uses_of_mild_depression_tablet")
        public int maxUsesOfMildDepressionTablet = 10;

        /**
         * 默认2，村民售卖安眠药片的所需等级
         */
        @SerializedName("lowest_level_of_insomnia_tablet")
        public int lowestLevelOfInsomniaTablet = 2;
        /**
         * 默认30，安眠药片的基础价格
         */
        @SerializedName("base_price_of_insomnia_tablet")
        public int basePriceOfInsomniaTablet = 30;
        /**
         * 默认10，安眠药片的最大库存量
         */
        @SerializedName("max_uses_of_insomnia_tablet")
        public int maxUsesOfInsomniaTablet = 10;

        /**
         * 默认2，村民售卖中度抗抑郁药片的所需等级
         */
        @SerializedName("lowest_level_of_moderate_depression_tablet")
        public int lowestLevelOfModerateDepressionTablet = 2;
        /**
         * 默认30，中度抗抑郁药片的基础价格
         */
        @SerializedName("base_price_of_moderate_depression_tablet")
        public int basePriceOfModerateDepressionTablet = 30;
        /**
         * 默认10，中度抗抑郁药片的最大库存量
         */
        @SerializedName("max_uses_of_moderate_depression_tablet")
        public int maxUsesOfModerateDepressionTablet = 10;

        /**
         * 默认3，村民售卖重度抗抑郁胶囊的所需等级
         */
        @SerializedName("lowest_level_of_mdd_capsule")
        public int lowestLevelOfMDDCapsule = 3;
        /**
         * 默认40，重度抗抑郁胶囊的基础价格
         */
        @SerializedName("base_price_of_mdd_capsule")
        public int basePriceOfMDDCapsule = 40;
        /**
         * 默认10，重度抗抑郁胶囊的最大库存量
         */
        @SerializedName("max_uses_of_mdd_capsule")
        public int maxUsesOfMDDCapsule = 10;

        /**
         * 默认3，村民售卖抗躁狂药片的所需等级
         */
        @SerializedName("lowest_level_of_mania_tablet")
        public int lowestLevelOfManiaTablet = 3;
        /**
         * 默认40，抗躁狂药片的基础价格
         */
        @SerializedName("base_price_of_mania_tablet")
        public int basePriceOfManiaTablet = 40;
        /**
         * 默认10，抗躁狂药片的最大库存量
         */
        @SerializedName("max_uses_of_mania_tablet")
        public int maxUsesOfManiaTablet = 10;
    }

    public static class DiaryConfig {
        /**
         * 默认false，启用后使用增强的日记生成器（实验性内容）
         * @see DiaryContentProducer
         */
        @SerializedName("enhanced_diary_generator")
        public boolean enhancedDiaryGenerator = false;

        /**
         * 默认3，日记本地化时查找的变体数量
         * @see DiaryContentProducer#findRandomVariant(String, int)
         */
        @SerializedName("diary_translation_key_variant_count")
        public int diaryTranslationKeyVariantCount = 3;

        /**
         * 默认false，启用后日记内容 <b>可能</b> 会变得更积极<br>
         * 其一：是如果消极感受得分高于积极感受得分，则将消极感受得分压缩20%<br>
         * 其二：是在生成日记时排除被标记为“由于消极属性应该被排除”的话题
         * @see FeelingProducer
         * @see DiaryContentProducer#generateTranslationKey(DiaryParagraph, boolean)
         */
        @SerializedName("gentle_mode")
        public boolean gentleMode = false;

        /**
         * 默认0.5，指数移动平均（EMA）因子，范围0.0到1.0
         * 越接近1.0，表示对最新数据的重视程度越高
         * @see TopicWeightCalculator
         */
        @SerializedName("ema_factor")
        public double EMAFactor = 0.5;
    }

    public static class ItemConfig {
        /**
         * 默认true，启用后使用精神健康量表将不再有冷却时间
         * @see MentalHealthScaleItemMixin
         */
        @SerializedName("disable_mental_health_scale_cooldown")
        public boolean disableMentalHealthScaleCooldown = false;
        /**
         * 默认true，启用后精神健康量表回复的文案将更“合理”
         * @see MentalHealthScaleItemMixin
         */
        @SerializedName("enhanced_mental_health_scale_action")
        public boolean enhancedMentalHealthScaleAction = true;
        /**
         * 默认true，启用后精神健康量表在使用后会消失
         * @see MentalHealthScaleItemMixin
         */
        @SerializedName("disposable_mental_health_scale")
        public boolean disposableMentalHealthScale = true;
    }

    public static class CombatConfig {
        /**
         * 默认false，启用后战斗状态下不会因为病情或困意闭眼/打盹
         * @see grainalcohol.dtt.mixin.event.MentalIllnessMixin
         */
        @SerializedName("safer_combat")
        public boolean saferCombat = false;
        /**
         * 默认false，启用后紧张性木僵不再禁用键盘输入，但会给予负面效果
         * @see ClientTickEventListenerMixin
         * @see grainalcohol.dtt.mixin.modification.KeyboardMixinMixin
         * @see grainalcohol.dtt.mixin.ServerPlayerEntityMixin
         */
        @SerializedName("safer_catatonic_stupor")
        public boolean saferCatatonicStupor = false;

        /**
         * 默认false，启用后无论情绪值如何，攻击或受击都将进入战斗状态
         * @see PlayerEventListenerMixin
         * @see PlayerEntityMixin
         */
        @SerializedName("easier_combat_state")
        public boolean easierCombatState = false;
    }

    public static class PTSDConfig {
        /**
         * 默认20，单位为方块，当玩家可能通过看到某个实体触发PTSD时，如果玩家与该实体之间的距离超过这个值，则不会触发PTSD
         * @see grainalcohol.dtt.mixin.modification.MentalStatusMixin
         */
        @SerializedName("max_distance_to_trigger_ptsd_by_sight")
        public int maxDistanceToTriggerPTSDBySight = 20;

        /**
         * depression原版为1.0，默认0.5，受伤时创伤值累积的倍率<br>
         * 该因子越大，受伤时创伤值增长越快，反之亦然
         * @see grainalcohol.dtt.mixin.modification.MentalStatusMixin
         */
        @SerializedName("ptsd_value_gain_multiplier")
        public double PTSDValueGainMultiplier = 0.5;

        /**
         * depression原版为1.0，默认1.0，任意等级PTSD的创伤值自然恢复的倍率<br>
         * 该因子越大，创伤值自然恢复越快，反之亦然
         * @see grainalcohol.dtt.mixin.modification.MentalStatusMixin
         */
        @SerializedName("ptsd_recovery_multiplier")
        public double PTSDRecoveryMultiplier = 1.0;

        /**
         * depression原版为1.0，默认1.5，潜伏期PTSD的创伤值自然恢复的倍率<br>
         * 该因子越大，潜伏期PTSD的创伤值自然恢复越快，反之亦然
         * @see grainalcohol.dtt.mixin.modification.MentalStatusMixin
         */
        @SerializedName("ptsd_recovery_multiplier_latent")
        public double PTSDRecoveryMultiplierLatent = 1.5;

        /**
         * depression原版为1.0，默认1.0，非潜伏期PTSD的创伤值自然恢复的倍率<br>
         * 该因子越大，非潜伏期PTSD的创伤值自然恢复越快，反之亦然
         * @see grainalcohol.dtt.mixin.modification.MentalStatusMixin
         */
        @SerializedName("ptsd_recovery_multiplier_active")
        public double PTSDRecoveryMultiplierActive  = 1.0;

        @SerializedName("comment_for_black_list")
        public String commentForBlackList = "To prevent certain damage types from triggering PTSD, add the damage type IDs in 'camelCase without namespace prefix' format. Only vanilla damage types are likely supported, " +
                "e.g., minecraft:generic_kill should be written as genericKill." + "Additionally, to prevent certain entities from triggering PTSD, add their full entity IDs, " +
                "e.g., minecraft:zombie (no changes needed here)." + "Finally, to prevent certain sound event mappings from triggering PTSD, add their mapping targets from config/depression/damagesource-sound-map.toml." +
                "Note: Damage types and entity IDs related to 'player' can never be blocked.";
        @SerializedName("comment_for_black_list_zh_cn")
        public String commentForBlackListZHCN = "如果你想阻止某些伤害类型触发PTSD，就把你需要的伤害类型的id的“不带命名空间前缀，使用首字母小写驼峰命名法”版本添加进去，而且大概率只能使用原版的伤害类型，" +
                "比如：minecraft:generic_kill 应该写成 genericKill。" + "再然后，如果你想阻止某些实体触发PTSD，就把它们完整的实体id添加进去，" +
                "比如：minecraft:zombie，这里不需要改。" + "最后，如果你想阻止某些声音事件映射触发PTSD，就把它们的映射目标添加进去，就是config/depression/damagesource-sound-map.toml里面的内容。" +
                "最后的最后，与“player”相关的伤害类型和实体id是永远无法被阻止的。";
        @SerializedName("universal_ptsd_black_list")
        public Set<String> universalPTSDBlackList = Set.of(
                "genericKill"
        );
    }

    public static class CommonConfig {
        /**
         * 默认false，启用后禁用精神特质选择界面
         * @see ClientTickEventListenerMixin
         */
        @SerializedName("disable_mental_trait_select_screen")
        public boolean disableMentalTraitSelectScreen = false;
        /**
         * 默认true，启用后使健康和躁狂状态的玩家不会触发精神疲劳，并使抗抑郁状态效果提供的额外触发概率翻倍
         * @see grainalcohol.dtt.mixin.event.MentalIllnessMixin
         */
        @SerializedName("mental_fatigue_trigger_chance_fixer")
        public boolean mentalFatigueTriggerChanceFixer = true;
        /**
         * depression原版为0.5，默认0.5，无聊值对情绪值恢复的影响强度<br>
         * 多次恢复的恢复原因相同时，值越大，递减的速度越快
         * @see grainalcohol.dtt.mixin.modification.MentalStatusMixin
         */
        @SerializedName("boredom_strength")
        public double boredomStrength = 0.5;
        /**
         * 默认2，无聊次数在此阈值内时恢复效果有45%加成
         */
        @SerializedName("novelty_threshold")
        public int noveltyThreshold = 2;
    }

    public enum NearbyMultipleBlocksHealMode {
        @SerializedName("everyone")
        EVERYONE,
        @SerializedName("max_only")
        MAX_ONLY,
        @SerializedName("nothing")
        NOTHING,
    }

    public enum NearbyAnythingHealMode {
        @SerializedName("everyone")
        EVERYONE,
        @SerializedName("exist")
        EXIST,
        @SerializedName("nothing")
        NOTHING,
    }
}
