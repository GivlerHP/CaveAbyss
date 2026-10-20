package ru.givler.caveabyss.core;

import net.minecraft.world.World;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import ru.givler.caveabyss.data.MinusOneLayer;

/** Main-thread snapshots consumed by Angelica's cloned section cache. */
public final class AngelicaHeightHooks {
    private AngelicaHeightHooks() { }

    public static int minimumSection(int original, World world) {
        return Math.min(original, MinusOneHooks.minimumY(world) >> 4);
    }

    public static int clampHeight(int y, World world) {
        return Math.max(MinusOneHooks.minimumY(world), Math.min(255, y));
    }

    public static boolean visuallyEmpty(boolean original, World world, int x, int y, int z) {
        if (y >= 0 || y < (MinusOneHooks.minimumY(world) >> 4)) return original;
        Chunk chunk = world.getChunkFromChunkCoords(x, z);
        return !MinusOneLayer.isClientChunkReady(chunk) || !MinusOneLayer.hasBlocksInSection(chunk, y + 4);
    }

    public static ExtendedBlockStorage section(Chunk chunk, int y) {
        if (y >= 0) {
            ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
            return y < sections.length ? sections[y] : null;
        }
        if (y < -4 || chunk.worldObj.provider.dimensionId != 0 || !MinusOneLayer.isClientChunkReady(chunk)) return null;
        int index = y + 4;
        ExtendedBlockStorage copy = new ExtendedBlockStorage(y << 4, !chunk.worldObj.provider.hasNoSky);
        copy.setBlockLSBArray(MinusOneLayer.copySection(chunk, index, 0));
        copy.setBlockMSBArray(new NibbleArray(MinusOneLayer.copySection(chunk, index, 1), 4));
        copy.setBlockMetadataArray(new NibbleArray(MinusOneLayer.copySection(chunk, index, 2), 4));
        copy.setBlocklightArray(new NibbleArray(MinusOneLayer.copyLightSection(chunk, EnumSkyBlock.Block, index), 4));
        if (!chunk.worldObj.provider.hasNoSky) {
            copy.setSkylightArray(new NibbleArray(MinusOneLayer.copyLightSection(chunk, EnumSkyBlock.Sky, index), 4));
        }
        copy.removeInvalidBlocks();
        return copy;
    }
}
