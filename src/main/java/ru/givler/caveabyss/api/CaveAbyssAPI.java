package ru.givler.caveabyss.api;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import ru.givler.caveabyss.data.MinusOneLayer;

/** Public entry point for mods that need height or section access. */
public final class CaveAbyssAPI {
    public static final int MAX_Y_EXCLUSIVE = 256;

    private CaveAbyssAPI() { }

    public static boolean hasDeepLayer(World world) {
        return world != null && world.provider != null && world.provider.dimensionId == 0;
    }

    public static int getMinY(World world) {
        return hasDeepLayer(world) ? MinusOneLayer.MIN_Y : 0;
    }

    public static int getMaxYExclusive(World world) {
        return MAX_Y_EXCLUSIVE;
    }

    public static IDeepWorld world(World world) {
        if (world == null) throw new IllegalArgumentException("world");
        return new WorldView(world);
    }

    public static IDeepChunk chunk(Chunk chunk) {
        if (chunk == null) throw new IllegalArgumentException("chunk");
        return new ChunkView(chunk);
    }

    public static IBlockSection section(Chunk chunk, int sectionY) {
        return chunk(chunk).getSection(sectionY);
    }

    private static void local(int coordinate, String name) {
        if (coordinate < 0 || coordinate > 15)
            throw new IndexOutOfBoundsException(name + " must be 0..15: " + coordinate);
    }

    private static final class WorldView implements IDeepWorld {
        private final World world;
        private WorldView(World world) { this.world = world; }
        @Override public World unwrap() { return world; }
        @Override public int getMinY() { return CaveAbyssAPI.getMinY(world); }
        @Override public int getMaxYExclusive() { return MAX_Y_EXCLUSIVE; }
        @Override public boolean containsY(int y) { return y >= getMinY() && y < MAX_Y_EXCLUSIVE; }
        @Override public Block getBlock(int x, int y, int z) { return world.getBlock(x, y, z); }
        @Override public int getMetadata(int x, int y, int z) {
            return world.getBlockMetadata(x, y, z);
        }
        @Override public TileEntity getTileEntity(int x, int y, int z) {
            return world.getTileEntity(x, y, z);
        }
        @Override public boolean setBlock(int x, int y, int z, Block block, int metadata, int flags) {
            return world.setBlock(x, y, z, block, metadata, flags);
        }
        @Override public void scheduleBlockUpdate(int x, int y, int z, Block block, int delay) {
            world.scheduleBlockUpdate(x, y, z, block, delay);
        }
        @Override public void scheduleBlockUpdateWithPriority(int x, int y, int z,
                                                               Block block, int delay, int priority) {
            world.scheduleBlockUpdateWithPriority(x, y, z, block, delay, priority);
        }
        @Override public boolean isChunkLoaded(int chunkX, int chunkZ) {
            return world.getChunkProvider().chunkExists(chunkX, chunkZ);
        }
        @Override public IDeepChunk getChunk(int chunkX, int chunkZ) {
            return chunk(world.getChunkFromChunkCoords(chunkX, chunkZ));
        }
    }

    private static final class ChunkView implements IDeepChunk {
        private final Chunk chunk;
        private ChunkView(Chunk chunk) { this.chunk = chunk; }
        @Override public Chunk unwrap() { return chunk; }
        @Override public int getMinSectionY() { return getMinY(chunk.worldObj) >> 4; }
        @Override public int getMaxSectionYExclusive() { return MAX_Y_EXCLUSIVE >> 4; }
        @Override public IBlockSection getSection(int sectionY) {
            if (sectionY < getMinSectionY() || sectionY >= getMaxSectionYExclusive())
                throw new IndexOutOfBoundsException("Section Y outside world: " + sectionY);
            return new SectionView(this, sectionY);
        }
        private void check(int x, int y, int z) {
            local(x, "x");
            local(z, "z");
            if (y < getMinY(chunk.worldObj) || y >= MAX_Y_EXCLUSIVE)
                throw new IndexOutOfBoundsException("Y outside world: " + y);
        }
        @Override public Block getBlock(int x, int y, int z) {
            check(x, y, z);
            return chunk.getBlock(x, y, z);
        }
        @Override public int getMetadata(int x, int y, int z) {
            check(x, y, z);
            return chunk.getBlockMetadata(x, y, z);
        }
        @Override public TileEntity getTileEntity(int x, int y, int z) {
            check(x, y, z);
            return chunk.worldObj.getTileEntity((chunk.xPosition << 4) + x, y,
                    (chunk.zPosition << 4) + z);
        }
        @Override public boolean setBlock(int x, int y, int z, Block block, int metadata, int flags) {
            check(x, y, z);
            return chunk.worldObj.setBlock((chunk.xPosition << 4) + x, y,
                    (chunk.zPosition << 4) + z, block, metadata, flags);
        }
    }

    private static final class SectionView implements IBlockSection {
        private final ChunkView chunk;
        private final int sectionY;
        private SectionView(ChunkView chunk, int sectionY) {
            this.chunk = chunk;
            this.sectionY = sectionY;
        }
        private int absolute(int y) {
            local(y, "y");
            return (sectionY << 4) + y;
        }
        @Override public int getSectionY() { return sectionY; }
        @Override public int getMinY() { return sectionY << 4; }
        @Override public Block getBlock(int x, int y, int z) {
            return chunk.getBlock(x, absolute(y), z);
        }
        @Override public int getMetadata(int x, int y, int z) {
            return chunk.getMetadata(x, absolute(y), z);
        }
        @Override public int getSavedLight(EnumSkyBlock type, int x, int y, int z) {
            local(x, "x");
            local(z, "z");
            if (type == null) throw new IllegalArgumentException("type");
            return chunk.chunk.getSavedLightValue(type, x, absolute(y), z);
        }
        @Override public boolean setBlock(int x, int y, int z, Block block, int metadata, int flags) {
            return chunk.setBlock(x, absolute(y), z, block, metadata, flags);
        }
    }
}
