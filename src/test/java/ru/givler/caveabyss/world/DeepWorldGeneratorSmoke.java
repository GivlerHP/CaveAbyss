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
        checkMigrationTransition();
        checkMountainCaves();
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

    private static void checkMigrationTransition() {
        for (Block cave : new Block[] {Blocks.air, Blocks.water}) {
            if (DeepWorldGenerator.migrationReplacement(Blocks.stone, 0, cave, 8) != cave
                    || DeepWorldGenerator.migrationReplacement(Blocks.bedrock, 0, cave, 0) != cave)
                throw new AssertionError("Migrated cave was capped at the old floor");
            for (Block protectedBlock : new Block[] {Blocks.diamond_ore, Blocks.chest,
                    Blocks.planks, Blocks.air, Blocks.lava, Blocks.water})
                if (DeepWorldGenerator.migrationReplacement(protectedBlock, 0, cave, 3) != protectedBlock)
                    throw new AssertionError("Migration replaced existing blocks");
            if (DeepWorldGenerator.migrationReplacement(Blocks.stone, 1, cave, 3) != Blocks.stone
                    || DeepWorldGenerator.migrationReplacement(Blocks.bedrock, 0, cave, 5) != Blocks.bedrock
                    || DeepWorldGenerator.migrationReplacement(Blocks.stone, 0, cave, 15) != Blocks.stone)
                throw new AssertionError("Migration exceeded its terrain limits");
        }
        if (DeepWorldGenerator.migrationReplacement(Blocks.bedrock, 0, Blocks.stone, 4) != Blocks.stone)
            throw new AssertionError("Old bedrock floor remained");
        for (Block old : new Block[] {Blocks.stone, Blocks.bedrock, Blocks.diamond_ore,
                Blocks.chest, Blocks.planks, Blocks.air, Blocks.water, Blocks.lava}) {
            for (Block cave : new Block[] {Blocks.air, Blocks.water}) {
                if (DeepWorldGenerator.migrationReplacement(old, 0, cave, 0, 0) != old)
                    throw new AssertionError("Preserve mode changed upper terrain");
                Block legacy = old == Blocks.bedrock ? Blocks.stone : old;
                if (DeepWorldGenerator.migrationReplacement(old, 0, cave, 0, 1) != legacy)
                    throw new AssertionError("Bedrock-only mode carved a cave");
                if (DeepWorldGenerator.migrationReplacement(old, 3, cave, 8, 3) != cave)
                    throw new AssertionError("Full migration preserved a cave obstruction");
                if (DeepWorldGenerator.migrationReplacement(old, 0, cave, 15, 3) != old)
                    throw new AssertionError("Full migration exceeded Y=14");
            }
            if (DeepWorldGenerator.migrationReplacement(old, 3, Blocks.stone, 8, 3) != old)
                throw new AssertionError("Full migration overwrote terrain outside a cave");
        }
    }

    private static void checkMountainCaves() {
        boolean[] mountain = new boolean[256];
        Arrays.fill(mountain, true);
        int air = 0, rock = 1, deep = 2, floor = 7, ore = 10, point = 11;
        int carved = 0, formations = 0, pillars = 0, lining = 0;
        for (int cx = -4; cx <= 4; cx++) {
            int[] terrain = new int[16384];
            Arrays.fill(terrain, deep);
            Arrays.fill(terrain, 0, 5 * 256, floor);
            terrain[(30 << 8) | 17] = ore;
            int[] original = terrain.clone();
            MountainCaves.carve(76543L, cx, -2, terrain, new boolean[256], rock, deep);
            if (!Arrays.equals(original, terrain)) throw new AssertionError("Mountain caves changed another biome");
            MountainCaves.carve(76543L, cx, -2, terrain, mountain, rock, deep);
            int[] repeat = original.clone();
            MountainCaves.carve(76543L, cx, -2, repeat, mountain, rock, deep);
            if (!Arrays.equals(repeat, terrain)) throw new AssertionError("Mountain cave generation is not deterministic");
            for (int state : terrain) if (state == air) carved++;
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                if (!MountainCaves.pillar(76543L, cx * 16 + x, -32 + z, 0)) continue;
                double center = MountainCaves.chamberCenter(76543L, cx * 16 + x, -32 + z);
                double half = MountainCaves.chamberHalfHeight(76543L, cx * 16 + x, -32 + z);
                if (half < 2) continue;
                boolean solid = true;
                for (int y = (int)Math.ceil(center - half); y <= (int)Math.floor(center + half); y++)
                    if (terrain[((y + 64) << 8) | (z << 4) | x] == 0) solid = false;
                if (!solid) throw new AssertionError("Mountain pillar was cut through by its chamber");
                pillars++;
            }
            MountainCaves.line(76543L, cx, -2, terrain, mountain, rock, deep, 12, 13);
            for (int state : terrain) if (state == 12 || state == 13) lining++;
            MountainCaves.decorate(76543L, cx, -2, terrain, mountain, rock, deep, 12, 13, point, 9);
            if (terrain[(30 << 8) | 17] != ore) throw new AssertionError("Mountain generation replaced ore");
            for (int i = 0; i < 5 * 256; i++)
                if (terrain[i] != floor) throw new AssertionError("Mountain cave reached bedrock");
            for (int y = 62; y < 64; y++) for (int column = 0; column < 256; column++)
                if (terrain[y * 256 + column] != deep && terrain[y * 256 + column] != 12
                        && terrain[y * 256 + column] != 13)
                    throw new AssertionError("Mountain cave cut the Y=0 boundary");
            for (int i = 0; i < terrain.length; i++) if ((terrain[i] & 65535) == point) {
                formations++;
                int support = i + ((terrain[i] & (8 << 16)) != 0 ? 256 : -256);
                if (support < 0 || support >= terrain.length || terrain[support] == 0)
                    throw new AssertionError("Generated dripstone has no support");
            }
        }
        if (carved < 1000 || formations < 5 || formations > carved / 40 || pillars == 0 || lining < 50)
            throw new AssertionError("Mountain chambers or formations missing: " + carved + "/" + formations
                    + ", pillars=" + pillars + ", lining=" + lining);
        double lowest = Double.MAX_VALUE, highest = -Double.MAX_VALUE;
        for (int x = -64; x < 64; x++) for (int z = -64; z < 64; z++) {
            double center = MountainCaves.chamberCenter(76543L, x, z);
            lowest = Math.min(lowest, center); highest = Math.max(highest, center);
            if (MountainCaves.chamberHalfHeight(76543L, x, z) > 6.5)
                throw new AssertionError("Mountain chamber exceeds the new height limit");
            if (MountainCaves.pillar(76543L, x, z, 0) && !MountainCaves.pillar(76543L, x, z, 1))
                throw new AssertionError("Pillar waist is wider than its anchors");
            if (Math.abs(MountainCaves.chamberCenter(76543L, x, z)
                    - MountainCaves.chamberCenter(76543L, x + 1, z)) > 1.5)
                throw new AssertionError("Hall floor has an abrupt discontinuity");
        }
        if (highest - lowest < 7) throw new AssertionError("Mountain chambers lack height variation");
        checkMountainLayers();
        if (MountainCaves.isMountain(net.minecraft.world.biome.BiomeGenBase.ocean)
                || !MountainCaves.isMountain(net.minecraft.world.biome.BiomeGenBase.extremeHills))
            throw new AssertionError("Mountain biome classifier failed");
        System.out.println("Mountain chambers, formations, deterministic generation and terrain boundaries passed");
    }

    private static void checkMountainLayers() {
        int separated = 0, tall = 0, passages = 0, samples = 0;
        for (int x = -256; x < 256; x += 4) for (int z = -256; z < 256; z += 4) {
            MountainCaves.Profile profile = MountainCaves.profile(76543L, x, z);
            samples++;
            int runs = 0, longest = 0, current = 0;
            for (int y = -51; y <= -3; y++) {
                boolean chamber = profile.vertical(y) <= 1;
                if (chamber) {
                    if (current == 0) runs++;
                    current++; longest = Math.max(longest, current);
                } else current = 0;
                if (!chamber && profile.tunnel(y)) passages++;
            }
            if (runs >= 2) separated++;
            if (longest >= 25) tall++;
            // Tunnel cross-sections must stay narrow, including at cell borders.
            if (Double.isFinite(profile.tunnelDistance)) {
                int height = 0;
                for (int y = -51; y <= -3; y++) if (profile.tunnel(y)) height++;
                if (height > 4) throw new AssertionError("Layer connector became a large shaft");
            }
        }
        if (separated < 100 || passages < 20 || tall == 0 || tall > samples / 12)
            throw new AssertionError("Missing layered caves, narrow passages or rare high halls: "
                    + separated + "/" + passages + "/" + tall);
        System.out.println("Separated mountain tiers, narrow rising passages and rare tall halls passed");
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
