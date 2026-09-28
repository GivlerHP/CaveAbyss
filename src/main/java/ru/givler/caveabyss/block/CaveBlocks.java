package ru.givler.caveabyss.block;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraftforge.oredict.OreDictionary;

public final class CaveBlocks {
    public static Block deepslate;
    public static Block cobbledDeepslate;
    public static Block polishedDeepslate;
    public static Block deepslateBricks;
    public static Block crackedDeepslateBricks;
    public static Block deepslateTiles;
    public static Block crackedDeepslateTiles;
    public static Block chiseledDeepslate;
    public static Block coalOre;
    public static Block ironOre;
    public static Block goldOre;
    public static Block redstoneOre;
    public static Block lapisOre;
    public static Block diamondOre;
    public static Block emeraldOre;

    private CaveBlocks() { }

    public static void register() {
        deepslate = register(new BlockDeepslate(), "deepslate");
        cobbledDeepslate = decorative("cobbled_deepslate");
        polishedDeepslate = decorative("polished_deepslate");
        deepslateBricks = decorative("deepslate_bricks");
        crackedDeepslateBricks = decorative("cracked_deepslate_bricks");
        deepslateTiles = decorative("deepslate_tiles");
        crackedDeepslateTiles = decorative("cracked_deepslate_tiles");
        chiseledDeepslate = decorative("chiseled_deepslate");
        OreDictionary.registerOre("stone", deepslate);
        OreDictionary.registerOre("cobblestone", cobbledDeepslate);
        coalOre = ore("deepslate_coal_ore", Blocks.coal_ore, 0);
        ironOre = ore("deepslate_iron_ore", Blocks.iron_ore, 1);
        goldOre = ore("deepslate_gold_ore", Blocks.gold_ore, 2);
        redstoneOre = ore("deepslate_redstone_ore", Blocks.redstone_ore, 2);
        lapisOre = ore("deepslate_lapis_ore", Blocks.lapis_ore, 1);
        diamondOre = ore("deepslate_diamond_ore", Blocks.diamond_ore, 2);
        emeraldOre = ore("deepslate_emerald_ore", Blocks.emerald_ore, 2);
    }

    private static Block ore(String name, Block vanilla, int level) {
        Block block = register(new BlockDeepslateOre(name, vanilla, level), name);
        String material = name.substring("deepslate_".length(), name.length() - "_ore".length());
        OreDictionary.registerOre("ore" + Character.toUpperCase(material.charAt(0))
                + material.substring(1), block);
        return block;
    }

    private static Block decorative(String name) {
        Block block = new Block(Material.rock) { }
                .setBlockName(name).setBlockTextureName("caveabyss:" + name)
                .setHardness(3.5F).setResistance(6.0F).setStepSound(Block.soundTypeStone);
        block.setHarvestLevel("pickaxe", 0);
        return register(block, name);
    }

    private static Block register(Block block, String name) {
        GameRegistry.registerBlock(block, name);
        return block;
    }
}
