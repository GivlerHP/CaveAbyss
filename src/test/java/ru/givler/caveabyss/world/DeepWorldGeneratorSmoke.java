package ru.givler.caveabyss.world;

import java.util.Arrays;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureMineshaftStart;
import java.util.Random;

public final class DeepWorldGeneratorSmoke {
    private DeepWorldGeneratorSmoke() { }

    public static void check() {
        Block[] shifted = new Block[65536];
        Block[] upper = new Block[65536];
        int[] lower = new int[16384];
        Block solid = new Block(Material.rock) { };
        Arrays.fill(shifted, solid);
        Arrays.fill(upper, solid);
        Arrays.fill(lower, 1);
        lower[9 << 8] = 7;
        shifted[10] = null; // Vanilla MapGenCaves writes null for cave air.
        shifted[64] = null; // A second cave pass must not cut the vanilla upper layer.
        shifted[((1 * 16 + 2) << 8) | 10] = null;

        DeepWorldGenerator.copyCarvedCaves(shifted, upper, lower);
        if (lower[10 << 8] != 0)
            throw new AssertionError("Vanilla cave air was not copied below Y=0");
        if (lower[9 << 8] != 7 || lower[11 << 8] != 1)
            throw new AssertionError("Cave copy altered solid terrain or bedrock");
        if (lower[(10 << 8) | (2 << 4) | 1] != 0
                || lower[(10 << 8) | (1 << 4) | 2] != 1)
            throw new AssertionError("Vanilla and negative X/Z layout mismatch");
        if (upper[0] != solid)
            throw new AssertionError("Lower cave pass altered vanilla terrain");
        checkCaveConnection();
        checkMineshaftPlacement();
        System.out.println("Vanilla cave air transfer passed");
    }

    private static void checkCaveConnection() {
        Block[] upper = new Block[65536];
        int[] lower = new int[16384];
        Arrays.fill(upper, Blocks.stone);
        Arrays.fill(lower, 1);
        int lowerOpening = ((-4 + 64) << 8) | (8 << 4) | 8;
        lower[lowerOpening] = 0;
        lower[((-5 + 64) << 8) | (8 << 4) | 8] = 7;
        upper[((8 * 16 + 8) << 8) | 4] = Blocks.air;
        DeepWorldGenerator.connectToVanillaCave(upper, lower, 1, 2, 12345L, 0, 0);
        boolean below = false, above = false;
        for (int x = 2; x < 14; x++) for (int z = 2; z < 14; z++) {
            if (lower[((-1 + 64) << 8) | (z << 4) | x] == 0) below = true;
            if (upper[((x * 16 + z) << 8)] == Blocks.air) above = true;
        }
        if (!below || !above)
            throw new AssertionError("Cave transition did not cross Y=0");
        if (lower[((-5 + 64) << 8) | (8 << 4) | 8] != 7)
            throw new AssertionError("Cave transition exposed bedrock");
    }

    private static void checkMineshaftPlacement() {
        Random random = new Random(71531L);
        StructureMineshaftStart start = new StructureMineshaftStart(null, random, 20, 20);
        DeepStructureHooks.placeDeepMineshaft(start, random);
        if (start.getBoundingBox().minY >= 0)
            throw new AssertionError("Deep mineshaft did not move below Y=0");
        if (start.getBoundingBox().minY < -58)
            throw new AssertionError("Deep mineshaft reaches bedrock");
        for (Object part : start.getComponents()) {
            StructureComponent component = (StructureComponent) part;
            if (component.getBoundingBox().minY < start.getBoundingBox().minY)
                throw new AssertionError("Mineshaft part was not moved with its start");
        }
    }
}
