package grainalcohol.dtt.mixin;

import grainalcohol.dtt.api.internal.SpawnResetHintController;
import grainalcohol.dtt.init.DTTPlayerState;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DTT 侧保存生命周期（S1）的注入点，与主包 {@code net.depression.mixin.PlayerListMixin} 打同一个方法：
 * 处理器方法一律带 {@code dtt$} 前缀，避免两个模组合并进同一个目标类时方法名撞车。
 * <ul>
 *   <li>{@code remove} HEAD：取消瞬态的"重置重生点"提示（不写 NBT、不跨会话）。放 HEAD 而不是 RETURN，
 *       是为了即便后面 {@code save} 抛异常导致 RETURN 不执行，也不会把待触发的提示带进下一个会话。</li>
 *   <li>{@code remove} RETURN：清理持久状态（该次 remove 自身的 save 已经写完）。</li>
 *   <li>{@code placeNewPlayer} HEAD：登录对账，清掉上次没走完的退出留下的残留（早于 load）。</li>
 * </ul>
 * 自动存档 / Esc 暂停 / {@code save-all} 都不经过这两个注入点，因此永远不会清内存。
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void dtt$cancelTransientHint(ServerPlayer player, CallbackInfo ci) {
        if (player instanceof SpawnResetHintController controller) {
            controller.dtt$cancelSpawnResetHint();
        }
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void dtt$onRemoveReturn(ServerPlayer player, CallbackInfo ci) {
        DTTPlayerState.onLogout((PlayerList) (Object) this, player);
    }

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void dtt$onLoginBegin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        DTTPlayerState.onLogin((PlayerList) (Object) this, player);
    }
}
