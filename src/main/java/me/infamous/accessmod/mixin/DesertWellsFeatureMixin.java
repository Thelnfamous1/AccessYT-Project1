package me.infamous.accessmod.mixin;

import me.infamous.accessmod.AccessMod;
import me.infamous.accessmod.common.registry.AccessModPOITypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.DesertWellsFeature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(DesertWellsFeature.class)
public class DesertWellsFeatureMixin {

    @Inject(method = "place(Lnet/minecraft/world/ISeedReader;Lnet/minecraft/world/gen/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/feature/NoFeatureConfig;)Z",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ISeedReader;setBlock(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", ordinal = 1, shift = At.Shift.AFTER))
    private void place$markPoi(ISeedReader seedReader, ChunkGenerator chunkGenerator, Random random, BlockPos blockPos, NoFeatureConfig config, CallbackInfoReturnable<Boolean> cir){
        seedReader.getLevel().getServer().execute(() -> {
                    AccessMod.LOGGER.info("Marking Desert Well at {}", blockPos);
                    seedReader.getLevel().getPoiManager().add(blockPos, AccessModPOITypes.DESERT_WELL.get());
                });

    }
}
