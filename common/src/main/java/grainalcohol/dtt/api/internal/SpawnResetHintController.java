package grainalcohol.dtt.api.internal;

/**
 * 瞬态"重置重生点"提示的取消接口。
 * <p>
 * 这条提示只活在服务端实体的内存字段里（不写 NBT、不跨会话），到期刻由
 * {@code ServerPlayerEntityMixin} 的 tick 注入倒计时触发（5 秒 = 100 tick，5 秒内重复设置合并成一次）。
 * 旧实现在 {@code addAdditionalSaveData} 里顺手清这个标记，属于"序列化回调带副作用"的老毛病：
 * 保存生命周期改造（D1）之后序列化是纯读，改由 {@code PlayerList.remove} 的 HEAD 显式取消，
 * 保证登出 / 关服后绝不会把待触发的提示带进下一个会话，也不依赖任何一次写盘。
 *
 * @see grainalcohol.dtt.mixin.ServerPlayerEntityMixin
 * @see grainalcohol.dtt.mixin.PlayerListMixin
 */
public interface SpawnResetHintController {
    /** 取消尚未到期的"重置重生点"提示（幂等，重复调用无副作用）。 */
    void dtt$cancelSpawnResetHint();
}
