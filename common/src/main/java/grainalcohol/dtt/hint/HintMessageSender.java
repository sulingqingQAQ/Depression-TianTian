package grainalcohol.dtt.hint;

import grainalcohol.dtt.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * <h1>HintMessageSender</h1>
 * <p>用于发送提示消息，自动处理重要性、发送与否、发送时机、变体选择和队列阻塞</p>
 *
 * <h2>触发消息</h2>
 * <p>通过 Sender 静态方法触发：</p>
 * <pre>{@code
 * if (someCondition()) {
 *     // 触发一个提示消息
 *     HintMessageSender.trigger(player, MyHintMessage.MY_SUBSCRIPTION_MESSAGE);
 * }
 * }</pre>
 * <pre>{@code
 * String someValue = "...";
 * if (someValue != null) {
 *     // 触发一个提示消息，并附加上下文数据
 *     HintMessageSender.trigger(player, MyHintMessage.MY_SUBSCRIPTION_MESSAGE, someValue);
 * }
 * }</pre>
 * <p>也可以直接在 SubscriptionHintMessage 实例上调用：</p>
 * <pre>{@code
 * MyHintMessage.MY_SUBSCRIPTION_MESSAGE.trigger(player);
 * MyHintMessage.MY_SUBSCRIPTION_MESSAGE.trigger(player, someValue);
 * }</pre>
 *
 * <h2>绕过冷却发送消息</h2>
 * <pre>{@code
 * if (someCondition()) {
 *     // 立即发送或创建一个提示消息任务
 *     HintMessageSender.send(player, MyHintMessage.MY_MESSAGE, someValue);
 * }
 * }</pre>
 *
 * <h2>afterSend 的执行时机</h2>
 * <p>消息的 afterSend（附带的心情效果等）在服务端实际发出提示文本之后执行一次，无论走哪个入口：
 * 重要消息在触发时立刻显示并执行；普通消息进入待发送队列时不执行，排到并显示时才执行；
 * 过期未显示的消息不执行。</p>
 */
public class HintMessageSender {
    private static final Random RANDOM = new Random();

    /**
     * 触发一个提示消息，程序自动处理是否发送和发送时机，可以填入上下文数据
     * @param player 要发送的目标
     * @param subscriptionHintMessage 要触发的消息
     * @param context 上下文数据，将填入本地化后的占位符
     */
    public static void trigger(ServerPlayer player, SubscriptionHintMessage subscriptionHintMessage, @NotNull String... context) {
        HintMessageManager.findInstance(player.getUUID(), subscriptionHintMessage.getIdentifier()).resetTimer();
        route(player, subscriptionHintMessage, context);
    }

    /**
     * 触发一个提示消息，程序自动处理是否发送和发送时机，可以填入上下文数据
     * @param player 要发送的目标
     * @param subscriptionHintMessage 要触发的消息
     * @param context 上下文数据，将填入本地化后的占位符
     */
    public static void trigger(ServerPlayer player, SubscriptionHintMessage subscriptionHintMessage, @NotNull Component... context) {
        trigger(player, subscriptionHintMessage, convertToStrings(context));
    }

    /**
     * 触发一个提示消息，程序自动处理是否发送和发送时机
     * @param player 要发送的目标
     * @param subscriptionHintMessage 要触发的消息
     */
    public static void trigger(ServerPlayer player, SubscriptionHintMessage subscriptionHintMessage) {
        trigger(player, subscriptionHintMessage, new String[0]);
    }

    /**
     * 绕过冷却时间机制，立刻创建一个提示消息任务，程序自动处理发送时机和生命周期
     * @param player 要发送的目标
     * @param hintMessage 要发送的消息
     * @param context 上下文数据，将填入本地化后的占位符
     */
    public static void send(ServerPlayer player, HintMessage hintMessage, @NotNull String... context) {
        route(player, hintMessage, context);
    }

    /**
     * 绕过冷却时间机制，立刻创建一个提示消息任务，程序自动处理发送时机和生命周期
     * @param player 要发送的目标
     * @param hintMessage 要发送的消息
     * @param context 上下文数据，将填入本地化后的占位符
     */
    public static void send(ServerPlayer player, HintMessage hintMessage, @NotNull Component... context) {
        send(player, hintMessage, convertToStrings(context));
    }

    /**
     * 绕过冷却时间机制，立刻创建一个提示消息任务，程序自动处理发送时机和生命周期
     * @param player 要发送的目标
     * @param hintMessage 要发送的消息
     */
    public static void send(ServerPlayer player, HintMessage hintMessage) {
        send(player, hintMessage, new String[0]);
    }

    /**
     * 绕过冷却时间机制，立刻创建一个提示消息任务，程序自动处理发送时机和生命周期
     * @param player 要发送的目标
     * @param sendable 要发送的消息
     */
    public static void send(ServerPlayer player, Sendable sendable) {
        route(player, sendable);
    }

    /**
     * 绕过冷却和队列机制，立刻发送一条提示消息，发送后执行一次 afterSend
     * @param player 要发送的目标
     * @param hintMessage 要发送的消息
     * @param context 上下文数据，将填入本地化后的占位符
     */
    public static void immediately(ServerPlayer player, HintMessage hintMessage, @NotNull String... context) {
        display(player, hintMessage, hintMessage.getTranslationKey(), hintMessage.getVariantCount(), context);
    }

    /**
     * 绕过冷却和队列机制，立刻发送一条提示消息，发送后执行一次 afterSend
     * @param player 要发送的目标
     * @param hintMessage 要发送的消息
     */
    public static void immediately(ServerPlayer player, HintMessage hintMessage) {
        immediately(player, hintMessage, new String[0]);
    }

    /**
     * 绕过冷却和队列机制，立刻发送一条提示消息，发送后执行一次 afterSend
     * @param player 要发送的目标
     * @param sendable 要发送的消息
     */
    public static void immediately(ServerPlayer player, Sendable sendable) {
        display(player, sendable.asHintMessage(), sendable.getTranslationKey(), sendable.getVariantCount(), sendable.getContext());
    }

    /**
     * 实际发出一条提示，然后执行一次该消息的 afterSend。<br>
     * afterSend 只在这里执行：重要消息立刻显示时执行一次；普通消息在待发送队列里排到、真正显示时执行一次；
     * 排队中、被阻塞、过期而没有显示的消息不执行。
     */
    private static void display(ServerPlayer player, HintMessage hintMessage, String translationKey, int variantCount, String[] context) {
        sendMessage(player, translationKey, variantCount, context);
        // afterSend 是 HintMessage 约定的可空回调（没有附带效果的消息返回 null）
        Consumer<ServerPlayer> afterSend = hintMessage.getAfterSend();
        if (afterSend != null) afterSend.accept(player);
    }

    /**
     * 将一个消息任务添加到待发送队列
     * @param player 要发送的目标
     * @param task 要添加的消息任务
     */
    public static void addPendingTask(ServerPlayer player, HintMessageTask task) {
        HintMessageManager.addPendingTask(player.getUUID(), task);
    }

    /**
     * 将一条提示消息添加到待发送队列
     * @param player 要发送的目标
     * @param hintMessage 要发送的消息
     * @param context 上下文数据，将填入本地化后的占位符
     */
    public static void addPendingTask(ServerPlayer player, HintMessage hintMessage, @NotNull String... context) {
        addPendingTask(player, new HintMessageTask(hintMessage, context));
    }

    /**
     * 将一条提示消息添加到待发送队列
     * @param player 要发送的目标
     * @param hintMessage 要发送的消息
     */
    public static void addPendingTask(ServerPlayer player, HintMessage hintMessage) {
        addPendingTask(player, hintMessage, new String[0]);
    }

    /**
     * 根据消息重要性路由：重要消息立刻发送并阻塞队列，非重要消息加入待发送队列
     */
    private static void route(ServerPlayer player, HintMessage hintMessage, String... context) {
        if (hintMessage.isImportant()) {
            immediately(player, hintMessage, context);
            HintMessageManager.blockQueue(player.getUUID());
        } else {
            addPendingTask(player, hintMessage, context);
        }
    }

    /**
     * 根据消息重要性路由，上下文数据来自 Sendable 本身
     */
    private static void route(ServerPlayer player, Sendable sendable) {
        if (sendable.isImportant()) {
            immediately(player, sendable);
            HintMessageManager.blockQueue(player.getUUID());
        } else {
            addPendingTask(player, sendable.asHintMessage(), sendable.getContext());
        }
    }

    private static String[] convertToStrings(Component[] texts) {
        String[] result = new String[texts.length];
        for (int i = 0; i < texts.length; i++) {
            result[i] = texts[i].getString();
        }
        return result;
    }

    protected static void sendMessage(ServerPlayer player, String translationKey, int variantCount) {
        player.displayClientMessage(Component.translatable(StringUtil.findTranslationKeyVariant(
                translationKey, variantCount, RANDOM
        )), true);
    }

    protected static void sendMessage(ServerPlayer player, String translationKey, int variantCount, @NotNull String... args) {
        if (args.length == 0) {
            sendMessage(player, translationKey, variantCount);
            return;
        }
        player.displayClientMessage(Component.translatable(StringUtil.findTranslationKeyVariant(
                translationKey, variantCount, RANDOM
        ), (Object[]) args), true);
    }
}
