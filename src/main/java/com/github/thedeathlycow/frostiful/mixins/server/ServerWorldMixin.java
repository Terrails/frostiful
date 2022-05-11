package com.github.thedeathlycow.frostiful.mixins.server;

import com.github.thedeathlycow.frostiful.config.WeatherConfig;
import com.github.thedeathlycow.frostiful.init.Frostiful;
import com.github.thedeathlycow.simple.config.Config;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.Collections;
import java.util.List;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

//    @Redirect(
//            method = "tickChunk",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Ljava/util/Random;nextInt(I)I",
//                    ordinal = 1
//            )
//    )
//    private int noRandomChanceForSnow(Random instance, int bound) {
//        //LostInTheCold.LOGGER.warn("If not in dev environment - delete noRandomChanceForSnow");
//        return 0;
//    }

    @Redirect(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"
            ),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/Biome;canSetSnow(Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;)Z")
            )
    )
    private boolean doSnowBuildup(ServerWorld instance, BlockPos blockPos, BlockState blockState) {
        BlockState current = instance.getBlockState(blockPos);
        Config config = WeatherConfig.CONFIG;
        int maxLayers = config.get(WeatherConfig.MAX_SNOW_BUILDUP);

        if (maxLayers == 0) {
            return false;
        }

        int layers = getLayersForPlacement(instance, current, blockPos, maxLayers);
        BlockState toSet = Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, layers);

        return instance.setBlockState(blockPos, toSet);
    }

    private int getSnowLayers(BlockState state) {
        if (state.isOf(Blocks.SNOW)) {
            return state.get(SnowBlock.LAYERS);
        } else if (state.isAir()) {
            return 0;
        } else {
            return SnowBlock.MAX_LAYERS;
        }
    }

    private int getLowestNeighbouringLayer(ServerWorld instance, BlockPos blockPos) {

        List<Integer> neighbourLayers = List.of(
                getSnowLayers(instance.getBlockState(blockPos.north())),
                getSnowLayers(instance.getBlockState(blockPos.south())),
                getSnowLayers(instance.getBlockState(blockPos.east())),
                getSnowLayers(instance.getBlockState(blockPos.west()))
        );

        return Collections.min(neighbourLayers);
    }

    private int getLayersForPlacement(ServerWorld instance, BlockState current, BlockPos blockPos, final int maxLayers) {

        int lowestNeighbour = getLowestNeighbouringLayer(instance, blockPos);
        int currentLayers = getSnowLayers(current);
        byte maxStep = WeatherConfig.CONFIG.get(WeatherConfig.MAX_SNOW_BUILDUP_STEP);
        if (currentLayers - lowestNeighbour < maxStep && currentLayers < maxLayers) {
            return Math.min(maxLayers, currentLayers + 1);
        }
        return currentLayers;
    }

}
