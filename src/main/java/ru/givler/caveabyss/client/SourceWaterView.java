package ru.givler.caveabyss.client;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;
import ru.givler.caveabyss.block.CaveBlocks;

/** Present submerged plants as source water only while drawing their water surface. */
final class SourceWaterView implements IBlockAccess {
    private final IBlockAccess world;

    SourceWaterView(IBlockAccess world) { this.world = world; }

    private boolean plant(int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return block == CaveBlocks.seagrass || block == CaveBlocks.tallSeagrass
                || block == CaveBlocks.kelp;
    }

    @Override public Block getBlock(int x, int y, int z) {
        return plant(x, y, z) ? Blocks.water : world.getBlock(x, y, z);
    }
    @Override public int getBlockMetadata(int x, int y, int z) {
        return plant(x, y, z) ? 0 : world.getBlockMetadata(x, y, z);
    }
    @Override public TileEntity getTileEntity(int x, int y, int z) {
        return world.getTileEntity(x, y, z);
    }
    @Override public int getLightBrightnessForSkyBlocks(int x, int y, int z, int light) {
        return world.getLightBrightnessForSkyBlocks(x, y, z, light);
    }
    @Override public boolean isAirBlock(int x, int y, int z) {
        return world.isAirBlock(x, y, z);
    }
    @Override public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return world.getBiomeGenForCoords(x, z);
    }
    @Override public int getHeight() { return world.getHeight(); }
    @Override public boolean extendedLevelsInChunkCache() {
        return world.extendedLevelsInChunkCache();
    }
    @Override public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean defaultValue) {
        return world.isSideSolid(x, y, z, side, defaultValue);
    }
    @Override public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
        return world.isBlockProvidingPowerTo(x, y, z, side);
    }
}
