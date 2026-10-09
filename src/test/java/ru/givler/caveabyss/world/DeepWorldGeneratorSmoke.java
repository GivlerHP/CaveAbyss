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
        net.minecraft.world.biome.BiomeGenBase reef = new net.minecraft.world.biome.BiomeGenBase(241) {};
        for (String name : new String[] {"Ocean", "Deep Ocean", "FrozenOcean", "Coral Reef", "Kelp Forest"}) {
            reef.biomeName = name;
            if (!OceanCaves.isOceanBiome(reef) || !DeepWorldGenerator.isOcean(reef))
                throw new AssertionError("Ocean generation/fauna classifier disagreement: " + name);
        }
        for (String name : new String[] {"Beach", "Tropics", "Mangrove", "River", "Bog"}) {
            reef.biomeName = name;
            if (OceanCaves.isOceanBiome(reef)) throw new AssertionError("Non-ocean classified as ocean: " + name);
        }

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
        int wet = 0, magma = 0, adjacentMagma = 0;
        for (int x = -256; x < 256; x++) for (int z = -256; z < 256; z++) {
            if (OceanCaves.flooded(12345L, x, z)) wet++;
            if (OceanCaves.magmaPatch(12345L, x, -30, z)) {
                magma++;
                if (OceanCaves.magmaPatch(12345L, x + 1, -30, z)) adjacentMagma++;
            }
        }
        if (wet < 30000 || wet > 220000) throw new AssertionError("Missing dry or flooded regions: " + wet);
        if (magma == 0 || magma > 262144 / 100 || adjacentMagma < magma / 3)
            throw new AssertionError("Magma must form rare connected patches: " + magma + "/" + adjacentMagma);
        checkOceanChambers();
        checkCaveConnection();
        checkMineshaftPlacement();
        System.out.println("Vanilla cave air transfer passed");
    }

    private static void checkOceanChambers() {
        boolean[] ocean = new boolean[256];
        Arrays.fill(ocean, true);
        int water = 9, rock = 2;
        int[] terrain = new int[16384];
        Arrays.fill(terrain, rock);
        for (int x = 3; x <= 12; x++) for (int z = 3; z <= 12; z++)
            for (int y = -40; y <= -30; y++) terrain[((y + 64) << 8) | (z << 4) | x] = water;
        OceanCaves.fillTerrain(12345L, 0, 0, terrain, ocean, water, rock);
        for (int x = 3; x <= 12; x++) for (int z = 3; z <= 12; z++)
            for (int y = -40; y <= -30; y++)
                if (terrain[((y + 64) << 8) | (z << 4) | x] != water)
                    throw new AssertionError("Large chamber was drained or divided by a dry-region barrier");

        Arrays.fill(terrain, rock);
        for (int x = 4; x <= 11; x++) for (int z = 4; z <= 11; z++)
            terrain[((-35 + 64) << 8) | (z << 4) | x] = 0;
        OceanCaves.fillTerrain(12345L, 0, 0, terrain, ocean, water, rock);
        if (terrain[((-35 + 64) << 8) | (8 << 4) | 8] != water)
            throw new AssertionError("Wide vanilla cavity must also fill with water");

        boolean dry = false;
        for (long seed = 0; seed < 100 && !dry; seed++) {
            Arrays.fill(terrain, rock);
            for (int z = 4; z <= 11; z++)
                terrain[((-35 + 64) << 8) | (z << 4) | 8] = 0;
            OceanCaves.fillTerrain(seed, 0, 0, terrain, ocean, water, rock);
            dry = terrain[((-35 + 64) << 8) | (8 << 4) | 8] == 0;
        }
        if (!dry) throw new AssertionError("Narrow tunnels cannot remain dry");
        System.out.println("Flooded broad chambers and optional dry narrow tunnels passed");
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
