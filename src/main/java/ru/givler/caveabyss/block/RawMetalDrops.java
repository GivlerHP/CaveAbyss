package ru.givler.caveabyss.block;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.oredict.OreDictionary;
import ru.givler.caveabyss.config.DeepOreConfig;

/** One stackable resource for each metal, independent of its stone host. */
public final class RawMetalDrops {
    public static final String[] METALS = {"Copper", "Tin", "Iron", "Silver", "Gold", "Tungsten"};
    private static final Item[] FALLBACK = new Item[METALS.length];
    private static final Item[] ACTIVE = new Item[METALS.length];

    public RawMetalDrops() { }

    public static void registerItems() {
        if (!DeepOreConfig.rawOreDrops || Loader.isModLoaded("minefantasy2")) return;
        for (int i = 0; i < METALS.length; i++) {
            String name = "raw_" + METALS[i].toLowerCase();
            FALLBACK[i] = new Item().setUnlocalizedName(name)
                    .setTextureName("caveabyss:" + name)
                    .setCreativeTab(CreativeTabs.tabMaterials);
            GameRegistry.registerItem(FALLBACK[i], name);
        }
    }

    public static void postInit() {
        boolean mf2 = Loader.isModLoaded("minefantasy2");
        for (int i = 0; i < METALS.length; i++) {
            ACTIVE[i] = mf2 ? GameRegistry.findItem("minefantasy2", "MF_Ore_ore" + METALS[i])
                    : FALLBACK[i];
            if (ACTIVE[i] == null) continue;
            if (DeepOreConfig.rawOreDrops) {
                // Ore recipes can accept the mined resource without a separate
                // recipe for every normal/deepslate block variant.
                OreDictionary.registerOre("ore" + METALS[i], new ItemStack(ACTIVE[i]));
                OreDictionary.registerOre("raw" + METALS[i], new ItemStack(ACTIVE[i]));
                copyResearchAndSmelting(i, mf2);
            }
        }
    }

    private static void copyResearchAndSmelting(int metal, boolean mf2) {
        Set<String> research = new HashSet<String>();
        ItemStack smelting = null;
        float experience = 0;
        ArrayList<ItemStack> sources = new ArrayList<ItemStack>(
                OreDictionary.getOres("ore" + METALS[metal]));
        if (metal == 2) sources.add(new ItemStack(Blocks.iron_ore));
        if (metal == 4) sources.add(new ItemStack(Blocks.gold_ore));
        for (ItemStack source : sources) {
            if (source == null || !(source.getItem() instanceof ItemBlock)) continue;
            for (int id : OreDictionary.getOreIDs(source)) {
                String name = OreDictionary.getOreName(id);
                if (name.startsWith("Artefact-") && research.add(name))
                    OreDictionary.registerOre(name, new ItemStack(ACTIVE[metal]));
            }
            if (!mf2 && smelting == null) {
                ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(source);
                if (result != null) {
                    smelting = result.copy();
                    experience = FurnaceRecipes.smelting().func_151398_b(source);
                }
            }
        }
        if (!mf2 && smelting != null)
            FurnaceRecipes.smelting().func_151394_a(new ItemStack(ACTIVE[metal]),
                    smelting, experience);
    }

    public static int metal(Block block, int metadata) {
        if (block == Blocks.iron_ore || block == CaveBlocks.ironOre) return 2;
        if (block == Blocks.gold_ore || block == CaveBlocks.goldOre) return 4;
        Object registryName = Block.blockRegistry.getNameForObject(block);
        if (registryName != null && registryName.toString().startsWith("minefantasy2:ore")) {
            String name = registryName.toString().substring("minefantasy2:ore".length());
            for (int i = 0; i < METALS.length; i++)
                if (name.equals(METALS[i])) return i;
        }
        ItemStack ore = new ItemStack(block, 1, metadata);
        for (int id : OreDictionary.getOreIDs(ore)) {
            String name = OreDictionary.getOreName(id);
            for (int i = 0; i < METALS.length; i++)
                if (name.equals("ore" + METALS[i])) return i;
        }
        return -1;
    }

    public static Item item(int metal) {
        return metal >= 0 && metal < ACTIVE.length ? ACTIVE[metal] : null;
    }

    public static int dropCount(Block block, int metal) {
        if (metal != 0 && metal != 1) return 1;
        Object name = Block.blockRegistry.getNameForObject(block);
        return name != null && name.toString().startsWith("caveabyss:deepslate_") ? 2 : 1;
    }

    public static ArrayList<ItemStack> directDrops(Block block, int metadata) {
        if (!DeepOreConfig.rawOreDrops) return null;
        int metal = metal(block, metadata);
        Item resource = item(metal);
        if (resource == null) return null;
        ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
        drops.add(new ItemStack(resource, dropCount(block, metal)));
        return drops;
    }

    @SubscribeEvent
    public void onHarvest(BlockEvent.HarvestDropsEvent event) {
        if (event.isSilkTouching) return;
        int metal = metal(event.block, event.blockMetadata);
        if (metal < 0) return;
        Item drop = DeepOreConfig.rawOreDrops ? item(metal) : null;
        if (DeepOreConfig.rawOreDrops && drop == null) return;
        event.drops.clear();
        if (DeepOreConfig.rawOreDrops) {
            event.drops.add(new ItemStack(drop, dropCount(event.block, metal)));
        } else event.drops.add(new ItemStack(event.block, 1, event.blockMetadata));
        event.dropChance = 1.0F;
    }
}
