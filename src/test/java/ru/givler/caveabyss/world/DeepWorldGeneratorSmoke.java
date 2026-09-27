package ru.givler.caveabyss.world;

import java.util.Arrays;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

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
        System.out.println("Vanilla cave air transfer passed");
    }
}
