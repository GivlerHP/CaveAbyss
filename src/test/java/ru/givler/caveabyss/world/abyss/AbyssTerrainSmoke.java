package ru.givler.caveabyss.world.abyss;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import ru.givler.caveabyss.block.BlockAbyssStone;
import ru.givler.caveabyss.block.BlockCaveFern;
import ru.givler.caveabyss.block.BlockCaveSpike;
import ru.givler.caveabyss.block.CaveBlocks;

public final class AbyssTerrainSmoke {
    public static void main(String[] args) throws Exception {
        if (!(AbyssTerrainSmoke.class.getClassLoader() instanceof net.minecraft.launchwrapper.LaunchClassLoader)) {
            java.net.URL[] urls = ((java.net.URLClassLoader) AbyssTerrainSmoke.class.getClassLoader()).getURLs();
            net.minecraft.launchwrapper.LaunchClassLoader loader = new net.minecraft.launchwrapper.LaunchClassLoader(urls);
            net.minecraft.launchwrapper.Launch.classLoader = loader;
            Thread.currentThread().setContextClassLoader(loader);
            loader.loadClass(AbyssTerrainSmoke.class.getName()).getMethod("main", String[].class)
                    .invoke(null, (Object) args);
            return;
        }
        cpw.mods.fml.common.Loader.injectData("7", "10", "99", "99", "1.7.10", "9.05",
                new File("build"), java.util.Collections.emptyList());
        net.minecraft.init.Bootstrap.func_151354_b();
        CaveBlocks.deepslate = new Block(Material.rock) { };
        CaveBlocks.abyssBedrock = new BlockAbyssStone("abyss_bedrock", "bedrock", 0x8247B5, true);
        CaveBlocks.abyssMoss = new BlockAbyssStone("abyss_moss", "grass_top", 0x45694B, false);
        CaveBlocks.caveFern = new BlockCaveFern();
        CaveBlocks.caveSpike = new BlockCaveSpike();
        CaveBlocks.magma = Blocks.netherrack;
        CaveBlocks.diamondOre = Blocks.diamond_ore;
        CaveBlocks.ironOre = Blocks.iron_ore;
        CaveBlocks.coalOre = Blocks.coal_ore;
        AbyssChunkProvider generator = new AbyssChunkProvider(null, 7158293, 0);
        int vines = 0, ferns = 0, spikes = 0, roots = 0;
        BufferedImage slice = new BufferedImage(768, 256, BufferedImage.TYPE_INT_RGB);
        long start = System.nanoTime();
        for (int cx = -24; cx < 24; cx++) {
            Block[] blocks = new Block[65536];
            byte[] metadata = new byte[65536];
            generator.generate(cx, 6, blocks, metadata);
            Block[] repeat = new Block[65536];
            byte[] repeatMeta = new byte[65536];
            generator.generate(cx, 6, repeat, repeatMeta);
            require(Arrays.equals(blocks, repeat) && Arrays.equals(metadata, repeatMeta), "Generation changed on repeat");
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 256; y++) {
                Block b = blocks[AbyssChunkProvider.index(x, y, z)];
                require(b != null, "Null terrain block");
                if (y < 4 || y > 251) require(b == CaveBlocks.abyssBedrock, "Decoration breached boundary");
                if (b == Blocks.vine) vines++;
                if (b == CaveBlocks.caveFern) ferns++;
                if (b == CaveBlocks.caveSpike) spikes++;
                if (b == Blocks.log) roots++;
                if (z == 0) slice.setRGB((cx + 24) * 16 + x, 255 - y, color(b));
            }
            int wx = cx * 16;
            if (Math.floorMod(wx, 192) == 96) {
                for (int y : new int[] {33, 225}) {
                    require(blocks[AbyssChunkProvider.index(8, y - 1, 0)] == CaveBlocks.deepslate, "Landing floor changed");
                    require(blocks[AbyssChunkProvider.index(8, y, 0)] == Blocks.air, "Landing obstructed");
                    require(blocks[AbyssChunkProvider.index(8, y + 1, 0)] == Blocks.air, "Landing headroom obstructed");
                }
                for (int y = 4; y <= 251; y++) {
                    require(blocks[AbyssChunkProvider.index(0, y, 0)] == Blocks.air, "Transfer shaft blocked");
                    require(blocks[AbyssChunkProvider.index(3, y, 0)] == Blocks.ladder, "Shaft ladder missing");
                }
            }
        }
        require(vines > 0 && ferns > 0 && spikes > 0 && roots > 0, "Missing cave decoration");
        NBTTagCompound input = new NBTTagCompound();
        input.setIntArray("Dimensions", new int[] {7, 13, 192});
        AbyssDimensions.Registry registry = new AbyssDimensions.Registry("test");
        registry.readFromNBT(input);
        NBTTagCompound output = new NBTTagCompound();
        registry.writeToNBT(output);
        require(Arrays.equals(input.getIntArray("Dimensions"), output.getIntArray("Dimensions")), "Registry round trip failed");
        for (int[] invalid : new int[][] {{0}, {-1}, {7,7}}) {
            input.setIntArray("Dimensions", invalid);
            boolean rejected = false;
            try { registry.readFromNBT(input); } catch (IllegalStateException expected) { rejected = true; }
            require(rejected, "Invalid dimension registry accepted");
        }
        File image = new File("build/reports/abyss/terrain-section.png");
        image.getParentFile().mkdirs();
        ImageIO.write(slice, "png", image);
        System.out.println("Abyss terrain OK: vines=" + vines + ", ferns=" + ferns + ", spikes=" + spikes
                + ", roots=" + roots + ", 96 chunks in " + (System.nanoTime() - start) / 1000000 + " ms");
    }

    private static int color(Block b) {
        if (b == Blocks.air) return 0x10171D;
        if (b == CaveBlocks.abyssBedrock) return 0x8247B5;
        if (b == CaveBlocks.abyssMoss) return 0x45694B;
        if (b == CaveBlocks.caveFern) return 0x80E398;
        if (b == Blocks.vine || b == Blocks.leaves) return 0x389660;
        if (b == CaveBlocks.caveSpike) return 0xB2B8AE;
        if (b == Blocks.ladder || b == Blocks.log) return 0xA68C57;
        if (b == CaveBlocks.magma) return 0xCC653D;
        return 0x545862;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
