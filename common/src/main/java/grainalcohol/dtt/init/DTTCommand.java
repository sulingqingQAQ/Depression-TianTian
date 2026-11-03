package grainalcohol.dtt.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import grainalcohol.dtt.api.event.SymptomEvent;
import grainalcohol.dtt.api.wrapper.PTSDLevel;
import grainalcohol.dtt.client.DTTServerConfigCache;
import grainalcohol.dtt.diary.dailystat.v2.DailyStat;
import grainalcohol.dtt.diary.dailystat.v2.DailyStatManager;
import grainalcohol.dtt.diary.topic.TopicWeightCalculator;
import grainalcohol.dtt.api.wrapper.MentalHealthStatus;
import net.depression.client.DepressionClient;
import net.depression.mental.MentalStatus;
import net.depression.network.CloseEyePacket;
import net.depression.server.Registry;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static grainalcohol.dtt.init.DTTListener.onPTSDLevelUp;
import static net.minecraft.commands.Commands.literal;

public class DTTCommand {
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            triggerCommand(dispatcher);
        });
    }

    private static void triggerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("depression")
                .then(literal("trigger")
                        .requires(source -> source.hasPermission(2))
                        .then(literal("close_eyes")
                                .executes(context -> triggerCloseEyes(context, false))
                                .then(literal("force")
                                        .executes(context -> triggerCloseEyes(context, true))
                                )
                        )
                        .then(literal("ptsd_0")
                                .executes(DTTCommand::triggerPTSD0)
                        )
                )
                .then(literal("check")
                        .requires(source -> source.hasPermission(2))
                        .then(literal("combat_status").executes(DTTCommand::checkCombatStatus))
                        .then(literal("is_close_eyes").executes(DTTCommand::checkIsCloseEyes))
                        .then(literal("daily_stat").executes(DTTCommand::checkDailyStat))
                        .then(literal("server_config_cache").executes(DTTCommand::checkServerConfigCache))
                )
                .then(literal("daily_stat")
                        .requires(source -> source.hasPermission(2))
                        .then(literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            CommandSourceStack source = context.getSource();
                                            int amount = IntegerArgumentType.getInteger(context.copyFor(source), "amount");
                                            return addDailyStat(context, amount);
                                }))

                        )
                )
        );
    }

    private static int addDailyStat(CommandContext<CommandSourceStack> context, int amount) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) return 0;
        DailyStat dailyStat = DailyStatManager.getTodayStat(player.getUUID());

        player.sendSystemMessage(Component.literal("Before amount is" + dailyStat.getNumberStat(DTTDailyStat.MONSTER_KILLED)));

        dailyStat.increaseNumberStat(DTTDailyStat.MONSTER_KILLED, amount);

        player.sendSystemMessage(Component.literal("Added monster killed daily stat by:" + amount));
        player.sendSystemMessage(Component.literal("Current amount is" + dailyStat.getNumberStat(DTTDailyStat.MONSTER_KILLED)));

        return 1;
    }

    private static int checkServerConfigCache(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) return 0;

        player.sendSystemMessage(Component.literal("Disable Mental Trait Select Screen: " + DTTServerConfigCache.disableMentalTraitSelectScreen));
        player.sendSystemMessage(Component.literal("Safer Catatonic Stupor: " + DTTServerConfigCache.saferCatatonicStupor));

        return 1;
    }

    private static int checkDailyStat(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) return 0;
        DailyStat dailyStat = DailyStatManager.getTodayStat(player.getUUID());

        player.sendSystemMessage(Component.literal("Current daily stat for player " + player.getName() + " : " + dailyStat.getNumberStatMap()));
        player.sendSystemMessage(Component.literal("Current weight for monster killed stat: " + TopicWeightCalculator.calculateWeight(player.getUUID(), DTTDailyStat.MONSTER_KILLED)));
        return 1;
    }

    private static int checkIsCloseEyes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) return 0;

        boolean isCloseEyes = DepressionClient.clientMentalStatus.mentalIllness.isCloseEye;
        player.sendSystemMessage(Component.literal(isCloseEyes ? "Eyes Closed" : "Eyes Open"));

        return 1;
    }

    private static int checkCombatStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) return 0;

        MentalStatus mentalStatus = Registry.mentalStatus.get(player.getUUID());
        player.sendSystemMessage(Component.literal(mentalStatus.combatCountdown > 0 ? "In Combat" : "Not in Combat"));

        return 1;
    }

    private static int triggerPTSD0(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) return 0;

        onPTSDLevelUp(player, "test_ptsd_name", PTSDLevel.LATENT);
        return 1;
    }

    private static int triggerCloseEyes(CommandContext<CommandSourceStack> context, boolean force) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) return 0;

        if (!force) {
            if (player.isCreative() || player.isSpectator()) {
                source.sendFailure(Component.translatable("commands.dtt.trigger.close_eyes.no_effect"));
                return 0;
            }
            if (!MentalHealthStatus.from(player).isDepressed()) {
                source.sendFailure(Component.translatable("commands.dtt.trigger.close_eyes.not_depressed"));
                return 0;
            }
        }

        CloseEyePacket.sendToPlayer(player);
        SymptomEvent.CLOSE_EYES_EVENT.invoker().onCloseEyes(player, false);
        return 1;
    }
}
