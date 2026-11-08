package grainalcohol.dtt.hint;

import grainalcohol.dtt.hint.timer.RandomizedTimer;
import grainalcohol.dtt.hint.timer.TimeUnit;
import grainalcohol.dtt.hint.timer.Timer;
import grainalcohol.dtt.hint.timer.TimerHolder;
import grainalcohol.dtt.registry.DTTRegistries;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class HintMessageManager {
    // 消息实例列表，玩家 UUID -> SendableManager (消息 ID -> 消息实例)，所有消息实例都在这里维护，其它位置使用Identifier引用即可
    private static final Map<UUID, SendableManager<HintMessageInstance>> hintMessageInstanceMap = new HashMap<>();
    // 待发送消息队列列表（非立即发送的消息最终都会来到这里），内部维护了生命周期计时器
    private static final Map<UUID, Queue<HintMessageTask>> pendingHintMessageTaskQueueMap = new ConcurrentHashMap<>();
    // 待发送消息轮询计时器列表
    private static final TimerHolder<UUID, Timer> pollingTimerHolder = new TimerHolder<>();
    // 被暂停发送队列的玩家列表（暂停期间不发送消息，直到下次轮询时尝试解除）
    private static final Set<UUID> blockedPendingQueues = new HashSet<>();

    // 默认的待发送消息轮询计时器（发送节奏控制）
    private static final RandomizedTimer DEFAULT_TIMER = RandomizedTimer.Builder
            .builder(10, TimeUnit.SECOND)
            .randomBound(5, TimeUnit.SECOND)
            .build();

    public static void onLogin(ServerPlayer player) {
        UUID playerUuid = player.getUUID();

        for (HintMessage hintMessage : DTTRegistries.GLOBAL_HINT_MESSAGE_REGISTRY) {
            hintMessageInstanceMap.computeIfAbsent(playerUuid, k -> new SendableManager<>())
                    .register(hintMessage.createInstance());
        }
        if (!pollingTimerHolder.containsKey(playerUuid)) pollingTimerHolder.add(playerUuid, DEFAULT_TIMER.copy());
        if (!pendingHintMessageTaskQueueMap.containsKey(playerUuid)) pendingHintMessageTaskQueueMap.put(playerUuid, new ArrayDeque<>());
    }

    /**
     * 清掉该玩家的全部提示状态（实例表 / 待发送队列 / 轮询计时器 / 阻塞标记）。<br>
     * 只应在"该次登出的存档已经写完"之后调用（{@code PlayerList.remove} 的 RETURN，见
     * {@code grainalcohol.dtt.init.DTTPlayerState#evict}）：PLAYER_QUIT 时还不能清，那时数据还没进 NBT。
     * 幂等；传 UUID 而不是 ServerPlayer，避免清理逻辑为了拿键去持有实体。
     */
    public static void onLogout(UUID playerUuid) {
        hintMessageInstanceMap.remove(playerUuid);
        pendingHintMessageTaskQueueMap.remove(playerUuid);
        pollingTimerHolder.remove(playerUuid);
        blockedPendingQueues.remove(playerUuid);
    }

    /** 清空全部玩家的提示状态（服务器停止 / 开始前的全量清扫，见 {@code DTTPlayerState#resetAll}）。 */
    public static void clearAll() {
        hintMessageInstanceMap.clear();
        pendingHintMessageTaskQueueMap.clear();
        // TimerHolder 没有 clear()：用它现成的 removeIf 一次清空（底层 ConcurrentHashMap）
        pollingTimerHolder.removeIf(timer -> true);
        blockedPendingQueues.clear();
    }

    public static void tick(ServerPlayer player) {
        UUID playerId = player.getUUID();
        // tick 所有实例的内部计时器
        // 发送逻辑由 HintMessageInstance 内部控制
        // 状态可能已被登出清理（清理在 remove RETURN，实体还可能被 tick 到）：实例表缺失就跳过，别让整条玩家 tick 链 NPE
        SendableManager<HintMessageInstance> instanceManager = hintMessageInstanceMap.get(playerId);
        if (instanceManager != null) {
            instanceManager.values().forEach(instance -> instance.tick(player));
        }

        // 处理过期机制，移除过期任务
        var queue = pendingHintMessageTaskQueueMap.get(playerId);
        if (queue != null && !queue.isEmpty()) {
            queue.forEach(task -> task.tick(player));
            queue.removeIf(HintMessageTask::isExpired);
        }

        // 只 tick 当前玩家自己的轮询计时器。
        // 原来是 pollingTimerHolder.tick(player)，把"该玩家"的计时器当成参数去 tick 全表，N 个玩家就是 N² 次 tick/s
        Timer pollingTimer = pollingTimerHolder.get(playerId);
        if (pollingTimer != null) pollingTimer.tick(player);

        // 处理待发送队列
        if (pollingTimerHolder.isFinished(playerId)) {
            if (blockedPendingQueues.contains(playerId)) {
                // 解除阻塞状态
                unblockQueue(playerId);
                // 本次轮询不发送消息，直接重置计时器等待下一次轮询
                pollingTimerHolder.reset(playerId);
            } else {
                // 发送一个待发送消息（如果有的话）
                sendNext(player);
                // 发送后重置轮询计时器
                pollingTimerHolder.reset(playerId);
            }
        }
    }

    private static void sendNext(ServerPlayer player) {
        var taskQueue = pendingHintMessageTaskQueueMap.get(player.getUUID());
        if (taskQueue == null || taskQueue.isEmpty()) return;

        taskQueue.poll().send(player);
    }

    public static void blockQueue(UUID playerId) {
        blockedPendingQueues.add(playerId);
    }

    public static void unblockQueue(UUID playerId) {
        blockedPendingQueues.remove(playerId);
    }

    public static HintMessageInstance findInstance(UUID playerId, HintMessage hintMessage) {
        return hintMessageInstanceMap.get(playerId).get(hintMessage.getIdentifier());
    }

    public static HintMessageInstance findInstance(UUID playerId, ResourceLocation hintMessageId) {
        return hintMessageInstanceMap.get(playerId).get(hintMessageId);
    }

    public static void addPendingTask(UUID playerId, HintMessageTask task) {
        pendingHintMessageTaskQueueMap.get(playerId).add(task);
    }

    public static void writeToNbt(UUID playerId, CompoundTag nbt) {
        // 空表保险：数据已被提前清理（或从未登录）时跳过整个 Hint 写入，不写半截 NBT，也不让整份玩家存档因 NPE 失败
        SendableManager<HintMessageInstance> instanceManager = hintMessageInstanceMap.get(playerId);
        Queue<HintMessageTask> taskQueue = pendingHintMessageTaskQueueMap.get(playerId);
        Timer pollingTimer = pollingTimerHolder.get(playerId);
        if (instanceManager == null || taskQueue == null || pollingTimer == null) return;

        ListTag instanceList = new ListTag();
        for (var instance : instanceManager.values()) {
            instanceList.add(instance.toNbt());
        }
//        hintMessageInstanceMap.remove(playerId);
        nbt.put("HintMessageInstances", instanceList);

        ListTag pendingList = new ListTag();
        for (var task : taskQueue) {
            pendingList.add(task.toNbt());
        }
//        pendingHintMessageTaskQueueMap.remove(playerId);
        nbt.put("PendingHintMessageTasks", pendingList);

        nbt.putInt("PollingTimerTicks", pollingTimer.getTicks());
        nbt.putBoolean("IsQueueBlocked", blockedPendingQueues.contains(playerId));
    }

    public static void readFromNbt(UUID playerId, CompoundTag nbt) {
        SendableManager<HintMessageInstance> instanceManager = new SendableManager<>();
        ListTag instanceList = nbt.getList("HintMessageInstances", Tag.TAG_COMPOUND);
        for (int i = 0; i < instanceList.size(); i++) {
            instanceManager.register(HintMessageInstance.fromNbt(instanceList.getCompound(i)));
        }
        hintMessageInstanceMap.put(playerId, instanceManager);

        Queue<HintMessageTask> taskQueue = new ArrayDeque<>();
        ListTag pendingList = nbt.getList("PendingHintMessageTasks", Tag.TAG_COMPOUND);
        for (int i = 0; i < pendingList.size(); i++) {
            taskQueue.add(HintMessageTask.fromNbt(pendingList.getCompound(i)));
        }
        pendingHintMessageTaskQueueMap.put(playerId, taskQueue);

        Timer pollingTimer = DEFAULT_TIMER.copy();
        pollingTimer.setTicks(nbt.getInt("PollingTimerTicks"));
        pollingTimerHolder.add(playerId, pollingTimer);

        if (nbt.getBoolean("IsQueueBlocked")) blockQueue(playerId);
    }
}
