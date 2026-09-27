package ru.givler.caveabyss.core;

import ru.givler.caveabyss.data.MinusOneLayer;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.tileentity.TileEntity;
import ru.givler.caveabyss.network.MinusOneNetwork;

public final class MinusOneHooks {
    private MinusOneHooks() { }

    public static Block getBlock(World world, int x, int y, int z) {
        return MinusOneLayer.getBlock(world, x, y, z);
    }

    public static int getMetadata(World world, int x, int y, int z) {
        return MinusOneLayer.getMetadata(world, x, y, z);
    }

    public static boolean blockExists(World world, int x, int y, int z) {
        return world.getChunkProvider().chunkExists(x >> 4, z >> 4);
    }

    public static boolean checkNegativeChunks(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int chunkX = x1 >> 4; chunkX <= x2 >> 4; chunkX++)
            for (int chunkZ = z1 >> 4; chunkZ <= z2 >> 4; chunkZ++)
                if (!world.getChunkProvider().chunkExists(chunkX, chunkZ)) return false;
        return true;
    }

    public static void serverMarkBlockForUpdate(WorldServer world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null) MinusOneNetwork.broadcastTileEntity(world, tile);
    }

    public static int getSavedLight(World world, EnumSkyBlock type, int x, int y, int z) {
        return MinusOneLayer.getLight(world, type, x, y, z);
    }

    public static int getSkyBrightness(World world, EnumSkyBlock type, int x, int y, int z) {
        if (type == EnumSkyBlock.Sky && world.provider.hasNoSky) return 0;
        if (!world.getBlock(x, y, z).getUseNeighborBrightness()) return getSavedLight(world, type, x, y, z);
        int value = world.getSavedLightValue(type, x, y + 1, z);
        value = Math.max(value, world.getSavedLightValue(type, x + 1, y, z));
        value = Math.max(value, world.getSavedLightValue(type, x - 1, y, z));
        value = Math.max(value, world.getSavedLightValue(type, x, y, z + 1));
        value = Math.max(value, world.getSavedLightValue(type, x, y, z - 1));
        return value;
    }

    public static int getBlockBrightness(World world, int x, int y, int z, boolean neighbors) {
        if (neighbors && world.getBlock(x, y, z).getUseNeighborBrightness()) {
            int value = world.getBlockLightValue_do(x, y + 1, z, false);
            value = Math.max(value, world.getBlockLightValue_do(x + 1, y, z, false));
            value = Math.max(value, world.getBlockLightValue_do(x - 1, y, z, false));
            value = Math.max(value, world.getBlockLightValue_do(x, y, z + 1, false));
            return Math.max(value, world.getBlockLightValue_do(x, y, z - 1, false));
        }
        int sky = world.provider.hasNoSky ? 0 : Math.max(0, MinusOneLayer.getLight(world, EnumSkyBlock.Sky, x, y, z) - world.skylightSubtracted);
        return Math.max(sky, MinusOneLayer.getLight(world, EnumSkyBlock.Block, x, y, z));
    }

    public static void setLight(World world, EnumSkyBlock type, int x, int y, int z, int value) {
        MinusOneLayer.setLight(world, type, x, y, z, value);
    }

    public static Block chunkGetBlock(Chunk chunk, int x, int y, int z) {
        return MinusOneLayer.getBlock(chunk, x, y, z);
    }

    public static int chunkGetMetadata(Chunk chunk, int x, int y, int z) {
        return MinusOneLayer.getMetadata(chunk, x, y, z);
    }

    public static boolean chunkSetBlock(Chunk chunk, int x, int y, int z, Block block, int metadata) {
        World world = chunk.worldObj;
        int worldX = (chunk.xPosition << 4) + x;
        int worldZ = (chunk.zPosition << 4) + z;
        Block previous = MinusOneLayer.getBlock(chunk, x, y, z);
        int previousMetadata = MinusOneLayer.getMetadata(chunk, x, y, z);
        if (previous == block && previousMetadata == metadata) return false;
        if (!world.isRemote) previous.onBlockPreDestroy(world, worldX, y, worldZ, previousMetadata);
        if (!MinusOneLayer.setBlock(chunk, worldX, y, worldZ, block, metadata, 0)) return false;
        if (!world.isRemote) {
            previous.breakBlock(world, worldX, y, worldZ, previous, previousMetadata);
        }
        TileEntity tile = chunk.getTileEntityUnsafe(x, y, z);
        if (tile != null && tile.shouldRefresh(previous, block, previousMetadata, metadata,
                world, worldX, y, worldZ)) {
            world.removeTileEntity(worldX, y, worldZ);
        }
        if (!world.isRemote) {
            block.onBlockAdded(world, worldX, y, worldZ);
        }
        if (block.hasTileEntity(metadata)) {
            tile = chunk.func_150806_e(x, y, z);
            if (tile != null) {
                tile.updateContainingBlockInfo();
                tile.blockMetadata = metadata;
                if (!world.isRemote) MinusOneNetwork.broadcastTileEntity(world, tile);
            }
        }
        return true;
    }

    public static boolean chunkSetMetadata(Chunk chunk, int x, int y, int z, int metadata) {
        World world = chunk.worldObj;
        int worldX = (chunk.xPosition << 4) + x;
        int worldZ = (chunk.zPosition << 4) + z;
        Block block = MinusOneLayer.getBlock(chunk, x, y, z);
        if (!MinusOneLayer.setBlock(chunk, worldX, y, worldZ, block, metadata, 0)) return false;
        if (block.hasTileEntity(metadata)) {
            TileEntity tile = chunk.func_150806_e(x, y, z);
            if (tile != null) {
                tile.updateContainingBlockInfo();
                tile.blockMetadata = metadata;
                if (!world.isRemote) MinusOneNetwork.broadcastTileEntity(world, tile);
            }
        }
        return true;
    }

    public static int chunkGetSavedLight(Chunk chunk, EnumSkyBlock type, int x, int y, int z) {
        return MinusOneLayer.getLight(chunk, type, x, y, z);
    }

    public static void chunkSetLight(Chunk chunk, EnumSkyBlock type, int x, int y, int z, int value) {
        MinusOneLayer.setLight(chunk, type, x, y, z, value);
    }

    public static int chunkGetBlockLight(Chunk chunk, int x, int y, int z, int skylightSubtracted) {
        int sky = chunk.worldObj.provider.hasNoSky ? 0 : Math.max(0,
                MinusOneLayer.getLight(chunk, EnumSkyBlock.Sky, x, y, z) - skylightSubtracted);
        return Math.max(sky, MinusOneLayer.getLight(chunk, EnumSkyBlock.Block, x, y, z));
    }
}
