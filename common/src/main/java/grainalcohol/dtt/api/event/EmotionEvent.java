package grainalcohol.dtt.api.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;

public interface EmotionEvent {
    /**
     * 玩家进入战斗状态事件
     * @see EnterCombatState#onEnterCombatState(ServerPlayer)
     */
    Event<EnterCombatState> ENTER_COMBAT_STATE_EVENT = EventFactory.createLoop();
    /**
     * 玩家退出战斗状态事件
     * @see ExitCombatState#onExitCombatState(ServerPlayer)
     */
    Event<ExitCombatState> EXIT_COMBAT_STATE_EVENT = EventFactory.createLoop();
    /**
     * 玩家进入禅定状态事件
     * @see EnterZenState#onEnterZenState(ServerPlayer)
     */
    Event<EnterZenState> ENTER_ZEN_STATE_EVENT = EventFactory.createLoop();
    /**
     * 玩家退出禅定状态事件
     * @see ExitZenState#onExitZenState(ServerPlayer)
     */
    Event<ExitZenState> EXIT_ZEN_STATE_EVENT = EventFactory.createLoop();
    /**
     * 玩家进入了一个BossBar通知范围，该事件可以被取消
     * @see ZenStateAddition#onZenStateChange(ServerBossEvent, ServerPlayer)
     */
    Event<ZenStateAddition> ZEN_STATE_ADDITION_EVENT = EventFactory.createEventResult();
    /**
     * 玩家离开了一个BossBar通知范围，该事件可以被取消
     * @see ZenStateRemoval#onZenStateChange(ServerBossEvent, ServerPlayer)
     */
    Event<ZenStateRemoval> ZEN_STATE_REMOVAL_EVENT = EventFactory.createEventResult();

    @FunctionalInterface
    interface EnterCombatState {
        void onEnterCombatState(ServerPlayer player);
    }

    @FunctionalInterface
    interface ExitCombatState {
        void onExitCombatState(ServerPlayer player);
    }

    @FunctionalInterface
    interface EnterZenState {
        void onEnterZenState(ServerPlayer player);
    }

    @FunctionalInterface
    interface ExitZenState {
        void onExitZenState(ServerPlayer player);
    }

    @FunctionalInterface
    interface ZenStateAddition {
        EventResult onZenStateChange(ServerBossEvent bossBar, ServerPlayer player);
    }

    @FunctionalInterface
    interface ZenStateRemoval {
        EventResult onZenStateChange(ServerBossEvent bossBar, ServerPlayer player);
    }
}
