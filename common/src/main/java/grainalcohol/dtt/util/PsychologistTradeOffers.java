package grainalcohol.dtt.util;

import grainalcohol.dtt.config.DTTConfig;
import grainalcohol.dtt.config.ServerConfig;
import net.depression.item.ModItems;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * 心理医生的可配置交易表，两个平台共用（Fabric 用 TradeOfferHelper 注册，NeoForge 塞进 VillagerTradesEvent）
 */
public class PsychologistTradeOffers {
    @FunctionalInterface
    public interface Sink {
        void accept(int lowestLevel, VillagerTrades.ItemListing listing);
    }

    public static void register(Sink sink) {
        ServerConfig.VillagerConfig villagerConfig = DTTConfig.getInstance().getServerConfig().villagerConfig;

        float priceMultiplier = villagerConfig.enablePriceFloating ? 1.0F : 0.0F;

        // mental health scale
        add(sink,
                villagerConfig.lowestLevelOfMentalHealthScale,
                villagerConfig.basePriceOfMentalHealthScale,
                new ItemStack(ModItems.MENTAL_HEALTH_SCALE.get(), 1),
                villagerConfig.maxUsesOfMentalHealthScale,
                5, priceMultiplier
        );
        // mild depression tablet
        add(sink,
                villagerConfig.lowestLevelOfMildDepressionTablet,
                villagerConfig.basePriceOfMildDepressionTablet,
                new ItemStack(ModItems.MILD_DEPRESSION_TABLET.get(), 6),
                villagerConfig.maxUsesOfMildDepressionTablet,
                5, priceMultiplier
        );
        // insomnia tablet
        add(sink,
                villagerConfig.lowestLevelOfInsomniaTablet,
                villagerConfig.basePriceOfInsomniaTablet,
                new ItemStack(ModItems.INSOMNIA_TABLET.get(), 6),
                villagerConfig.maxUsesOfInsomniaTablet,
                20, priceMultiplier
        );
        // moderate depression tablet
        add(sink,
                villagerConfig.lowestLevelOfModerateDepressionTablet,
                villagerConfig.basePriceOfModerateDepressionTablet,
                new ItemStack(ModItems.MODERATE_DEPRESSION_TABLET.get(), 6),
                villagerConfig.maxUsesOfModerateDepressionTablet,
                20, priceMultiplier
        );
        // MDD capsule
        add(sink,
                villagerConfig.lowestLevelOfMDDCapsule,
                villagerConfig.basePriceOfMDDCapsule,
                new ItemStack(ModItems.MDD_CAPSULE.get(), 6),
                villagerConfig.maxUsesOfMDDCapsule,
                40, priceMultiplier
        );
        // mania tablet
        add(sink,
                villagerConfig.lowestLevelOfManiaTablet,
                villagerConfig.basePriceOfManiaTablet,
                new ItemStack(ModItems.MANIA_TABLET.get(), 6),
                villagerConfig.maxUsesOfManiaTablet,
                40, priceMultiplier
        );
    }

    private static void add(Sink sink, int lowestLevel, int emeraldCost, ItemStack sellItem, int maxUses, int merchantExperience, float priceMultiplier) {
        sink.accept(lowestLevel, (entity, randomSource) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCost), sellItem.copy(),
                maxUses, merchantExperience, priceMultiplier)
        );
    }
}
