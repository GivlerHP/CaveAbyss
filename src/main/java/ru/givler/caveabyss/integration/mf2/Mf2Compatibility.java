package ru.givler.caveabyss.integration.mf2;

import cpw.mods.fml.common.registry.GameRegistry;
import java.util.ArrayList;
import java.util.List;
import minefantasy.mf2.api.crafting.refine.BloomRecipe;
import minefantasy.mf2.api.refine.Alloy;
import minefantasy.mf2.api.refine.AlloyRecipes;
import minefantasy.mf2.api.refine.BigFurnaceRecipes;
import minefantasy.mf2.item.tool.drops.DropChance;
import minefantasy.mf2.item.tool.drops.OreDropController;
import minefantasy.mf2.knowledge.ArtefactCategories;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.oredict.OreDictionary;

/** Runs after MF2 has populated its recipes, research and handpick tables. */
public final class Mf2Compatibility {
    private Mf2Compatibility() { }

    public static void register() {
        alias(Blocks.coal_ore, 0, deep("deepslate_coal_ore"), 0);
        alias(Blocks.iron_ore, 0, deep("deepslate_iron_ore"), 0);
        alias(Blocks.gold_ore, 0, deep("deepslate_gold_ore"), 0);
        alias(Blocks.redstone_ore, 0, deep("deepslate_redstone_ore"), 0);
        alias(Blocks.lapis_ore, 0, deep("deepslate_lapis_ore"), 0);
        alias(Blocks.diamond_ore, 0, deep("deepslate_diamond_ore"), 0);
        alias(Blocks.emerald_ore, 0, deep("deepslate_emerald_ore"), 0);
        Block thaum = GameRegistry.findBlock("Thaumcraft", "blockCustomOre");
        Block deepThaum = deep("deepslate_thaumcraft_ore");
        if (thaum != null && deepThaum != null)
            for (int meta = 0; meta < 8; meta++) alias(thaum, meta, deepThaum, meta);
        Block bop = GameRegistry.findBlock("BiomesOPlenty", "gemOre");
        Block deepBop = deep("deepslate_bop_ore");
        if (bop != null && deepBop != null)
            for (int gem = 0; gem < 7; gem++) alias(bop, (gem + 1) * 2, deepBop, gem);
        String[] mf2 = {"copper", "tin", "silver", "tungsten", "nitre", "sulfur", "mythic"};
        for (String name : mf2) {
            Block source = GameRegistry.findBlock("minefantasy2", "ore" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
            Block variant = deep("deepslate_mf2_" + name + "_ore");
            if (source != null && variant != null) alias(source, 0, variant, 0);
        }
    }

    private static Block deep(String name) {
        return GameRegistry.findBlock("caveabyss", name);
    }

    private static void alias(Block source, int sourceMeta, Block variant, int variantMeta) {
        if (variant == null) return;
        ItemStack original = new ItemStack(source, 1, sourceMeta);
        ItemStack copy = new ItemStack(variant, 1, variantMeta);
        DropChance chance = OreDropController.getDropChance(original);
        if (chance != null) OreDropController.register(copy, chance);
        for (int id : OreDictionary.getOreIDs(original)) {
            String name = OreDictionary.getOreName(id);
            if (name.startsWith("Artefact-")) OreDictionary.registerOre(name, copy.copy());
        }
        if (source == Blocks.iron_ore) ArtefactCategories.METALS_ORE_I.add(copy.copy());
        if (source == GameRegistry.findBlock("minefantasy2", "oreMythic"))
            ArtefactCategories.MYTHYC.add(copy.copy());
        ItemStack furnace = FurnaceRecipes.smelting().getSmeltingResult(original);
        if (furnace != null) FurnaceRecipes.smelting().func_151394_a(copy, furnace.copy(),
                FurnaceRecipes.smelting().func_151398_b(original));
        ItemStack bloom = BloomRecipe.getSmeltingResult(original);
        if (bloom != null) BloomRecipe.addRecipe(copy.copy(), bloom.copy());
        BigFurnaceRecipes big = BigFurnaceRecipes.getResult(original);
        if (big != null) BigFurnaceRecipes.addRecipe(copy.copy(), big.result.copy(), big.tier);
        copyAlloys(original, copy);
    }

    @SuppressWarnings("unchecked")
    private static void copyAlloys(ItemStack original, ItemStack variant) {
        List<Alloy> recipes = new ArrayList<Alloy>(AlloyRecipes.alloys);
        for (Alloy recipe : recipes) {
            List<ItemStack> ingredients = new ArrayList<ItemStack>();
            boolean replaced = false;
            for (Object ingredient : recipe.recipeItems) {
                ItemStack stack = (ItemStack) ingredient;
                if (stack.getItem() == original.getItem()
                        && (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE
                            || stack.getItemDamage() == original.getItemDamage())) {
                    ingredients.add(variant.copy());
                    replaced = true;
                } else ingredients.add(stack.copy());
            }
            if (replaced) AlloyRecipes.addAlloy(recipe.recipeOutput.copy(), recipe.level, ingredients);
        }
    }
}
