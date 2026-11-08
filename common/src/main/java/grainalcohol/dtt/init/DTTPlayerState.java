package grainalcohol.dtt.init;

import grainalcohol.dtt.DTTMod;
import grainalcohol.dtt.diary.dailystat.v2.DailyStatManager;
import grainalcohol.dtt.diary.topic.v2.TopicManager;
import grainalcohol.dtt.hint.HintMessageManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.UUID;

/**
 * DTT 侧的保存生命周期（S1）：什么时候清、清什么。
 * <ul>
 *   <li>NBT 序列化回调是纯读，绝不清理（见 {@code grainalcohol.dtt.mixin.ServerPlayerEntityMixin}）。</li>
 *   <li>登出清理只在 {@code PlayerList.remove} 的 RETURN —— 即该次 remove 自身的 save 之后。</li>
 *   <li>登录（{@code placeNewPlayer} HEAD）只清"没有在线旧会话"的残留，兜住 RETURN 没执行到的异常路径。</li>
 *   <li>服务器停止后 / 开始前（{@code LifecycleEvent.SERVER_STOPPED / SERVER_BEFORE_START}）全量清扫。</li>
 *   <li>单人房主在 remove RETURN 不清，留到 SERVER_STOPPED：单人服随即停止，且
 *       {@code IntegratedPlayerList.playerData}（写 level.dat 用）之后仍可能被重新捕获，
 *       保留内存状态能保证任何后续捕获都是完整数据。</li>
 *   <li>死亡重生 / 换维度不经过 remove，不触发任何清理。</li>
 * </ul>
 * 与主包 {@code net.depression.server.PlayerSaveLifecycle} 相互独立：DTT 不看写盘结果，
 * 失败快照由主包持有整份 NBT（DTT 键在其中），因此这里也**不需要**抬高对主包的版本依赖。
 */
public final class DTTPlayerState {
    private static final String TAG = "[DTT/Save]";

    private DTTPlayerState() {
    }

    /**
     * {@code PlayerList.remove} 的 RETURN：该次 remove 自己的 {@code save} 已经执行完，可以清内存。
     * 同 UUID 已有更新（或仍在线）的会话时不动——注册表现在属于它。
     */
    public static void onLogout(PlayerList playerList, ServerPlayer player) {
        UUID playerId = player.getUUID();
        try {
            if (playerList.getPlayer(playerId) != null) {
                DTTMod.LOGGER.warn("{} logout uuid={} cleanup skipped (a session with the same uuid is online)", TAG, playerId);
                return;
            }
            if (player.server.isSingleplayerOwner(player.getGameProfile())) {
                DTTMod.LOGGER.debug("{} logout uuid={} owner=true, keep state until SERVER_STOPPED", TAG, playerId);
                return;
            }
            evict(playerId);
            DTTMod.LOGGER.debug("{} logout uuid={} name={} evicted", TAG, playerId, player.getGameProfile().getName());
        } catch (RuntimeException e) {
            // 清理失败不能打断 remove：残留由登录对账与 SERVER_STOPPED 兜底
            DTTMod.LOGGER.error("{} logout cleanup failed uuid={}", TAG, playerId, e);
        }
    }

    /**
     * {@code PlayerList.placeNewPlayer} 的 HEAD（早于 {@code PlayerList.load}）：登录 = 干净起点。
     * 同 UUID 没有在线旧会话时，上一次没走完的退出留下的残留一律作废；有在线旧会话则不动。
     */
    public static void onLogin(PlayerList playerList, ServerPlayer player) {
        UUID playerId = player.getUUID();
        try {
            if (playerList.getPlayer(playerId) != null) {
                DTTMod.LOGGER.warn("{} login uuid={} while a session with the same uuid is still online, keep state", TAG, playerId);
                return;
            }
            evict(playerId);
        } catch (RuntimeException e) {
            DTTMod.LOGGER.error("{} login cleanup failed uuid={}", TAG, playerId, e);
        }
    }

    /** 清掉该玩家的全部持久状态（提示实例/队列/计时器、每日统计、历史话题）。幂等。 */
    public static void evict(UUID playerId) {
        HintMessageManager.onLogout(playerId);
        DailyStatManager.remove(playerId);
        TopicManager.remove(playerId);
    }

    /** 服务器停止后 / 开始前的全量清扫（单人房主的持久状态也只在这里清）。幂等。 */
    public static void resetAll() {
        HintMessageManager.clearAll();
        DailyStatManager.clearAll();
        TopicManager.clearAll();
        DTTMod.LOGGER.debug("{} reset all in-memory player state", TAG);
    }
}
