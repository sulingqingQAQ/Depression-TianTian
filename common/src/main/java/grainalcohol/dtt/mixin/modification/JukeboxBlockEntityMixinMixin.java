package grainalcohol.dtt.mixin.modification;

import com.bawnorton.mixinsquared.TargetHandler;
import grainalcohol.dtt.config.DTTConfig;
import grainalcohol.dtt.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = JukeboxBlockEntity.class, priority = 1500)
public class JukeboxBlockEntityMixinMixin {
    @TargetHandler(
            mixin = "net.depression.mixin.emotion.JukeboxBlockEntityMixin",
            name = "tick"
    )
    @ModifyConstant(
            method = "@MixinSquared:Handler",
            constant = @Constant(longValue = 20L)
    )
    // 1.21.1 主包的 JukeboxBlockEntityMixin.tick 跟随原版 static ticker 改成了 static handler，挂在它上面的注入也必须是 static
    private static long modifyJukeboxMentalHealInterval(long constant) {
        return DTTConfig.getInstance().getServerConfig().mentalHealConfig.nearbyJukeboxIntervalTicks;
    }

    @TargetHandler(
            mixin = "net.depression.mixin.emotion.JukeboxBlockEntityMixin",
            name = "tick"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true
    )
    // 目标 handler：static tick(Level, BlockPos, BlockState, JukeboxBlockEntity, CallbackInfo)V，参数照抄后再接自己的 CallbackInfo
    private static void injectJukeboxTick(Level level, BlockPos blockPos, BlockState blockState, JukeboxBlockEntity jukebox, CallbackInfo originalCi, CallbackInfo ci) {
        // 原版逻辑是EVERYONE模式
        if (DTTConfig.getInstance().getServerConfig().mentalHealConfig.nearbyJukeboxMode != ServerConfig.NearbyAnythingHealMode.EVERYONE) {
            ci.cancel();
        }
    }
}
