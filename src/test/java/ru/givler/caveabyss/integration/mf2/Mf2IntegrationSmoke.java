package ru.givler.caveabyss.integration.mf2;

import java.util.Arrays;

public final class Mf2IntegrationSmoke {
    private Mf2IntegrationSmoke() { }

    public static void check() {
        int[] terrain = new int[16384];
        Arrays.fill(terrain, 1);
        int x = 8, z = 8, y = -60;
        int index = ((y + 64) << 8) | (z << 4) | x;
        if (Mf2Integration.adjacent(terrain, x, y, z, 7))
            throw new AssertionError("Mythic would generate away from bedrock");
        terrain[index - 256] = 7;
        if (!Mf2Integration.adjacent(terrain, x, y, z, 7))
            throw new AssertionError("Bedrock below mythic ore was not detected");
        terrain[index - 256] = 1;
        terrain[index + 1] = 0;
        if (!Mf2Integration.adjacent(terrain, x, y, z, 0))
            throw new AssertionError("Air beside nitre ore was not detected");
        if (Mf2Integration.adjacent(terrain, x, y, z, 7))
            throw new AssertionError("Air was mistaken for bedrock");
        System.out.println("MF2 ore neighbor conditions passed");
    }
}
