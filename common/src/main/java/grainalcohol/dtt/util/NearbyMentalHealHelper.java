package grainalcohol.dtt.util;

import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class NearbyMentalHealHelper {
    @Nullable
    public static JukeboxBlockEntity findNearestPlayingJukeboxEntity(ServerPlayer player, int radius) {
        JukeboxBlockEntity playingJukebox = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        ServerLevel world = player.serverLevel();
        BlockPos center = player.blockPosition();
        Vec3 playerPos = player.position();

        for (BlockPos checkPos : BlockPos.withinManhattan(center, radius, 2, radius)) {
            if (world.getBlockEntity(checkPos) instanceof JukeboxBlockEntity jukeboxBlockEntity
                    && jukeboxBlockEntity.getSongPlayer().isPlaying()
                    && !isBlockedByBlocks(world, player.getEyePosition(), checkPos, player)
            ) {
                // 这个唱片机至少存在、正在播放且没有被其他方块或液体阻挡
                double distanceSquared = playerPos.distanceToSqr(Vec3.atCenterOf(checkPos));
                if (distanceSquared < nearestDistanceSquared) {
                    // 这个唱片机比之前找到的更近
                    playingJukebox = jukeboxBlockEntity;
                    nearestDistanceSquared = distanceSquared;
                }
            }
        }

        return playingJukebox;
    }

    public static boolean isBlockNearby(ServerPlayer player, Predicate<BlockState> predicate, int radius) {
        ServerLevel world = player.serverLevel();
        BlockPos center = player.blockPosition();

        for (BlockPos checkPos : BlockPos.withinManhattan(center, radius, 2, radius)) {
            // 曼哈顿距离
            if (predicate.test(world.getBlockState(checkPos)) && !isBlockedByBlocks(world, player.getEyePosition(), checkPos, player)) {
                // 有这样一个方块，并且玩家与这个方块之间没有被其他方块或液体阻挡
                return true;
            }
        }

        return false;
    }

    public static boolean isBlockNearby(ServerPlayer player, Block block, int radius) {
        return isBlockNearby(player, state -> state.is(block), radius);
    }

    public static boolean isBlockNearby(ServerPlayer player, Class<? extends Block> blockClass, int radius) {
        return isBlockNearby(player, state -> blockClass.isAssignableFrom(state.getBlock().getClass()), radius);
    }

    public static boolean isBlockNearby(ServerPlayer player, TagKey<Block> blockTag, int radius) {
        return isBlockNearby(player, state -> state.is(blockTag), radius);
    }

    public static boolean isPetNearby(ServerPlayer player, double radius) {
        ServerLevel world = player.serverLevel();
        AABB searchBox = player.getBoundingBox().inflate(radius + 1);

        for (TamableAnimal entity : world.getEntitiesOfClass(TamableAnimal.class, searchBox, TamableAnimal::isTame)) {
            if (entity.getOwnerUUID() != null
                    && entity.getOwnerUUID().equals(player.getUUID())
                    && entity.distanceToSqr(player) <= radius * radius
                    && !isBlockedByBlocks(world, player.getEyePosition(), entity.getEyePosition(), player)
            ) {
                // 有这样一个宠物，它的主人是发起检查的玩家
                // 并且玩家与这个宠物的距离不超过指定半径
                // 以及玩家与这个宠物之间没有被其他方块或液体阻挡
                return true;
            }
        }

        return false;
    }

    public static boolean isBlockedByBlocks(ServerLevel world, Vec3 start, BlockPos end, Entity entity) {
        return isBlockedByBlocks(world, start, Vec3.atCenterOf(end), entity);
    }

    public static boolean isBlockedByBlocks(ServerLevel world, Vec3 start, Vec3 end, Entity entity) {
        BlockHitResult hit = world.clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,  // 检测碰撞箱
                ClipContext.Fluid.ANY, // 检测液体
                entity
        ));

        if (hit.getType() == HitResult.Type.MISS) {
            // 没有击中任何东西
            return false;
        }

        // 击中了目标方块本身
        BlockPos hitPos = hit.getBlockPos();
        if (hitPos.getCenter().distanceToSqr(end) < 0.01) {
            return false;
        }

        // 击中了其他方块
        return true;
    }
}
