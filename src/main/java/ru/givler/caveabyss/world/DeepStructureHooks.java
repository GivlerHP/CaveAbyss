package ru.givler.caveabyss.world;

import java.util.Random;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureMineshaftStart;
import net.minecraft.block.Block;
import ru.givler.caveabyss.block.CaveBlocks;

/** Extends vanilla structure placement into the generated negative layer. */
public final class DeepStructureHooks {
    private static final Map<IChunkProvider, DeepMineshaftGenerator> MINESHAFTS =
            new WeakHashMap<IChunkProvider, DeepMineshaftGenerator>();
    private DeepStructureHooks() { }

    public static int dungeonY(Random random, World world) {
        return world.provider.dimensionId == 0 ? random.nextInt(320) - 64 : random.nextInt(256);
    }

    public static int lavaLakeY(int vanillaY, Random random, World world) {
        return world.provider.dimensionId == 0 && random.nextBoolean()
                ? -48 + random.nextInt(40) : vanillaY;
    }

    public static Block dungeonWall(World world, int y, Block vanilla) {
        return world.provider.dimensionId == 0 && y < 0 ? CaveBlocks.cobbledDeepslate : vanilla;
    }

    private static synchronized DeepMineshaftGenerator mineshafts(IChunkProvider provider) {
        DeepMineshaftGenerator generator = MINESHAFTS.get(provider);
        if (generator == null) {
            generator = new DeepMineshaftGenerator();
            MINESHAFTS.put(provider, generator);
        }
        return generator;
    }

    public static void prepareMineshafts(IChunkProvider provider, World world, int chunkX, int chunkZ) {
        if (world.provider.dimensionId != 0) return;
        DeepMineshaftGenerator generator = mineshafts(provider);
        try {
            generator.func_151539_a(provider, world, chunkX, chunkZ, null);
        } finally {
            generator.releaseWorld();
        }
    }

    public static void populateMineshafts(IChunkProvider provider, World world, int chunkX, int chunkZ) {
        if (world.provider.dimensionId != 0) return;
        DeepMineshaftGenerator generator = mineshafts(provider);
        Random random = new Random(world.getSeed() ^ ((long) chunkX * 341873128712L)
                ^ ((long) chunkZ * 132897987541L) ^ 0xD33F5A17L);
        try {
            generator.generateStructuresInChunk(world, random, chunkX, chunkZ);
        } finally {
            generator.releaseWorld();
        }
    }

    public static void placeDeepMineshaft(StructureMineshaftStart start, Random random) {
        StructureBoundingBox box = start.getBoundingBox();
        int height = box.getYSize();
        // Large vanilla networks cross Y=0; their lower corridors remain above bedrock.
        int minY = height <= 56 ? -58 + random.nextInt(57 - height) : -58;
        int offset = minY - box.minY;
        box.offset(0, offset, 0);
        for (Object part : start.getComponents())
            ((StructureComponent) part).getBoundingBox().offset(0, offset, 0);
    }
}
