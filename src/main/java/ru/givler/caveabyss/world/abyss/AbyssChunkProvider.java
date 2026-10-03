package ru.givler.caveabyss.world.abyss;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import ru.givler.caveabyss.block.CaveBlocks;

public final class AbyssChunkProvider implements IChunkProvider {
    private final World world;
    private final int depth;
    private final long seed;
    private final FractalCaves caves;

    public AbyssChunkProvider(World world, int depth) {
        this(world, world.getSeed(), depth);
    }

    AbyssChunkProvider(World world, long seed, int depth) {
        this.world = world;
        this.seed = seed;
        this.depth = depth;
        caves = new FractalCaves(seed, depth);
    }

    public static int index(int x, int y, int z) { return (x * 16 + z) * 256 + y; }

    public void generate(int chunkX, int chunkZ, Block[] blocks, byte[] metadata) {
        int bx = chunkX * 16, bz = chunkZ * 16;
        Random random = new Random(seed ^ chunkX * 341873128712L
                ^ chunkZ * 132897987541L ^ depth * 7199369L);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int wx = bx + x, wz = bz + z;
            FractalCaves.Column column = caves.column(wx, wz);
            int dx = Math.floorMod(wx, FractalCaves.SHAFT_SPACING) - 96;
            int dz = Math.floorMod(wz, FractalCaves.SHAFT_SPACING) - 96;
            for (int y = 0; y < 256; y++) {
                int i = index(x, y, z);
                if (y < 4 || y > 251 || dx == 4 && dz == 0) blocks[i] = CaveBlocks.abyssBedrock;
                else if (dx == 3 && dz == 0) { blocks[i] = Blocks.ladder; metadata[i] = 4; }
                else blocks[i] = column.air(y) ? Blocks.air : CaveBlocks.deepslate;
                if (blocks[i] == CaveBlocks.deepslate && random.nextInt(420) == 0)
                    blocks[i] = y < 48 ? CaveBlocks.diamondOre : y < 110 ? CaveBlocks.ironOre : CaveBlocks.coalOre;
            }
            if (caves.shaftRadius(wx, wz) < 14) continue;
            for (int y = 17; y < 238; y++) {
                int i = index(x, y, z);
                if (blocks[i] != Blocks.air) continue;
                if (blocks[i - 1] == CaveBlocks.deepslate) {
                    if (column.moisture > -0.12) {
                        blocks[i - 1] = CaveBlocks.abyssMoss;
                        if (random.nextInt(4) == 0) blocks[i] = CaveBlocks.caveFern;
                        else if (random.nextInt(16) == 0) blocks[i] = Blocks.brown_mushroom;
                    } else if (random.nextInt(70) == 0) blocks[i - 1] = CaveBlocks.magma;
                    if (blocks[i] == Blocks.air && random.nextInt(32) == 0)
                        spike(blocks, metadata, i, 1, 2 + random.nextInt(4));
                }
                if (blocks[i + 1] == CaveBlocks.deepslate) {
                    if (random.nextInt(12) == 0) spike(blocks, metadata, i, -1, 3 + random.nextInt(7));
                    else if (column.moisture > 0 && random.nextInt(20) == 0) {
                        int length = 2 + random.nextInt(6);
                        for (int n = 0; n < length && y - n > 17 && blocks[i - n] == Blocks.air; n++) {
                            blocks[i - n] = n < 2 ? Blocks.log : Blocks.leaves;
                            metadata[i - n] = (byte) (n < 2 ? 3 : 7);
                        }
                    }
                }
            }
        }
        // Sample neighboring columns directly, including across chunk edges.
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int wx = bx + x, wz = bz + z;
            if (caves.shaftRadius(wx, wz) < 14 || caves.moisture(wx, wz) < -0.15) continue;
            FractalCaves.Column east = caves.column(wx + 1, wz), west = caves.column(wx - 1, wz);
            FractalCaves.Column north = caves.column(wx, wz - 1), south = caves.column(wx, wz + 1);
            for (int y = 235; y > 18; y--) {
                int i = index(x, y, z);
                if (blocks[i] != Blocks.air || random.nextInt(9) != 0) continue;
                int face = !east.air(y) ? 8 : !west.air(y) ? 2 : !north.air(y) ? 4 : !south.air(y) ? 1 : 0;
                if (face == 0) continue;
                int length = 3 + random.nextInt(11);
                for (int n = 0; n < length && y - n > 16 && blocks[i - n] == Blocks.air; n++) {
                    blocks[i - n] = Blocks.vine;
                    metadata[i - n] = (byte) face;
                }
            }
        }
    }

    private static void spike(Block[] blocks, byte[] meta, int start, int direction, int length) {
        for (int n = 0; n < length; n++) {
            int i = start + n * direction;
            if (blocks[i] != Blocks.air) break;
            blocks[i] = CaveBlocks.caveSpike;
            meta[i] = (byte) (Math.min(3, n * 4 / length) | (direction < 0 ? 4 : 0));
        }
    }

    @Override public Chunk provideChunk(int x, int z) {
        Block[] blocks = new Block[65536];
        byte[] meta = new byte[blocks.length];
        generate(x, z, blocks, meta);
        Chunk chunk = new Chunk(world, blocks, meta, x, z);
        Arrays.fill(chunk.getBiomeArray(), (byte) BiomeGenBase.jungle.biomeID);
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override public void populate(IChunkProvider provider, int cx, int cz) {
        Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 17; y < 240; y++) {
            Block block = chunk.getBlock(x, y, z);
            if (block == CaveBlocks.caveFern || block == CaveBlocks.magma)
                world.func_147451_t(cx * 16 + x, y, cz * 16 + z);
        }
        if (!world.getGameRules().getGameRuleBooleanValue("doMobSpawning")) return;
        Random random = new Random(world.getSeed() ^ cx * 17274907L ^ cz * 341873128712L ^ depth);
        if (random.nextInt(3) != 0) return;
        // Only this chunk is read or written; decoration cannot cascade into new chunks.
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = cx * 16 + 2 + random.nextInt(12), z = cz * 16 + 2 + random.nextInt(12);
            int y = 20 + random.nextInt(210);
            for (; y > 17; y--) {
                if (world.getBlock(x, y - 1, z) != CaveBlocks.abyssMoss
                        || !world.isAirBlock(x, y, z) || !world.isAirBlock(x, y + 1, z)) continue;
                int kind = random.nextInt(4);
                EntityLiving animal = kind == 0 ? new EntityBat(world) : kind == 1 ? new EntityChicken(world)
                        : kind == 2 ? new EntityPig(world) : new EntitySheep(world);
                animal.setLocationAndAngles(x + 0.5, y, z + 0.5, random.nextFloat() * 360, 0);
                if (!world.getCollidingBoundingBoxes(animal, animal.boundingBox).isEmpty()) break;
                animal.onSpawnWithEgg(null);
                animal.func_110163_bv();
                world.spawnEntityInWorld(animal);
                return;
            }
        }
    }

    @Override public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
        if (type == EnumCreatureType.ambient)
            return Collections.singletonList(new BiomeGenBase.SpawnListEntry(EntityBat.class, 10, 1, 3));
        return Collections.emptyList();
    }
    @Override public boolean chunkExists(int x, int z) { return true; }
    @Override public Chunk loadChunk(int x, int z) { return provideChunk(x, z); }
    @Override public boolean saveChunks(boolean all, IProgressUpdate progress) { return true; }
    @Override public void saveExtraData() { }
    @Override public boolean unloadQueuedChunks() { return false; }
    @Override public boolean canSave() { return true; }
    @Override public String makeString() { return "Fractal Abyss " + depth; }
    @Override public int getLoadedChunkCount() { return 0; }
    @Override public void recreateStructures(int x, int z) { }
    @Override public ChunkPosition func_147416_a(World world, String name, int x, int y, int z) { return null; }
}
