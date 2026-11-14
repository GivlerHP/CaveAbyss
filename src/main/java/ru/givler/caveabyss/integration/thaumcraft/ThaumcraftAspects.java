package ru.givler.caveabyss.integration.thaumcraft;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Explicit cave block/item tags; ore values follow their original stone-host counterparts. */
public final class ThaumcraftAspects {
    private ThaumcraftAspects() {}
    public static void register() {
        for (String stone : new String[]{"deepslate", "cobbled_deepslate", "polished_deepslate",
                "deepslate_bricks", "cracked_deepslate_bricks", "deepslate_tiles",
                "cracked_deepslate_tiles", "chiseled_deepslate", "smooth_basalt", "calcite",
                "dripstone_block", "pointed_dripstone"}) tag(stone, a(Aspect.EARTH, 2));
        tag("deepslate_coal_ore", a(Aspect.EARTH, 1, Aspect.ENERGY, 2, Aspect.FIRE, 1));
        tag("deepslate_iron_ore", a(Aspect.EARTH, 1, Aspect.METAL, 3));
        tag("deepslate_gold_ore", a(Aspect.EARTH, 1, Aspect.METAL, 2, Aspect.GREED, 1));
        tag("deepslate_redstone_ore", a(Aspect.EARTH, 1, Aspect.ENERGY, 2, Aspect.MECHANISM, 2));
        tag("deepslate_lapis_ore", a(Aspect.EARTH, 1, Aspect.SENSES, 3));
        tag("deepslate_diamond_ore", a(Aspect.EARTH, 1, Aspect.GREED, 3, Aspect.CRYSTAL, 3));
        tag("deepslate_emerald_ore", a(Aspect.EARTH, 1, Aspect.GREED, 4, Aspect.CRYSTAL, 3));
        tag("amethyst_shard", a(Aspect.CRYSTAL, 2, Aspect.SENSES, 1));
        tag("amethyst_block", a(Aspect.CRYSTAL, 6, Aspect.SENSES, 2));
        tag("budding_amethyst", a(Aspect.CRYSTAL, 6, Aspect.SENSES, 2, Aspect.LIFE, 1));
        tag("small_amethyst_bud", a(Aspect.CRYSTAL, 1, Aspect.SENSES, 1));
        tag("medium_amethyst_bud", a(Aspect.CRYSTAL, 2, Aspect.SENSES, 1));
        tag("large_amethyst_bud", a(Aspect.CRYSTAL, 3, Aspect.SENSES, 1));
        tag("amethyst_cluster", a(Aspect.CRYSTAL, 4, Aspect.SENSES, 2));
        tag("magma", a(Aspect.EARTH, 1, Aspect.FIRE, 3));
        tag("seagrass", a(Aspect.PLANT, 1, Aspect.WATER, 1));
        tag("tall_seagrass", a(Aspect.PLANT, 1, Aspect.WATER, 1));
        tag("kelp", a(Aspect.PLANT, 1, Aspect.WATER, 1));
        tag("small_aquamarine_bud", a(Aspect.CRYSTAL, 1, Aspect.WATER, 1));
        tag("medium_aquamarine_bud", a(Aspect.CRYSTAL, 2, Aspect.WATER, 1));
        tag("large_aquamarine_bud", a(Aspect.CRYSTAL, 3, Aspect.WATER, 1));
        tag("aquamarine_cluster", a(Aspect.CRYSTAL, 4, Aspect.WATER, 2));

        // Raw drops are registered only without MF2; do not alter MF2's own resources.
        tag("raw_copper", a(Aspect.METAL, 2, Aspect.EARTH, 1, Aspect.EXCHANGE, 1));
        tag("raw_tin", a(Aspect.METAL, 3, Aspect.ENTROPY, 1, Aspect.CRYSTAL, 1));
        tag("raw_iron", a(Aspect.METAL, 3, Aspect.EARTH, 1));
        tag("raw_silver", a(Aspect.METAL, 3, Aspect.ENTROPY, 1, Aspect.GREED, 1));
        tag("raw_gold", a(Aspect.METAL, 2, Aspect.EARTH, 1, Aspect.GREED, 1));
        tag("raw_tungsten", a(Aspect.METAL, 4, Aspect.EARTH, 2, Aspect.MINE, 3));
        tag("deepslate_mf2_copper_ore", a(Aspect.METAL, 2, Aspect.EARTH, 1, Aspect.EXCHANGE, 1));
        tag("deepslate_mf2_tin_ore", a(Aspect.METAL, 3, Aspect.ENTROPY, 1, Aspect.CRYSTAL, 1));
        tag("deepslate_mf2_silver_ore", a(Aspect.METAL, 3, Aspect.ENTROPY, 1, Aspect.GREED, 1));
        tag("deepslate_mf2_tungsten_ore", a(Aspect.METAL, 4, Aspect.EARTH, 2, Aspect.MINE, 3));
        tag("deepslate_mf2_nitre_ore", a(Aspect.EARTH, 2, Aspect.ENERGY, 2));
        tag("deepslate_mf2_sulfur_ore", a(Aspect.EARTH, 2, Aspect.FIRE, 2));
        tag("deepslate_mf2_mythic_ore", a(Aspect.EARTH, 2, Aspect.METAL, 4, Aspect.MAGIC, 3));
        tag("deepslate_mf2_deralium_ore", a(Aspect.EARTH, 2, Aspect.METAL, 4, Aspect.MAGIC, 3));
        for (int meta = 0; meta < 7; meta++) tag("deepslate_bop_ore", meta,
                a(Aspect.EARTH, 1, Aspect.CRYSTAL, 3, Aspect.GREED, 3));
        tag("deepslate_thaumcraft_ore", 0, a(Aspect.EARTH, 1, Aspect.METAL, 2, Aspect.EXCHANGE, 2, Aspect.POISON, 1));
        Aspect[] primal = {Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY};
        for (int i = 0; i < primal.length; i++) tag("deepslate_thaumcraft_ore", i + 1,
                a(Aspect.EARTH, 1, primal[i], 3, Aspect.CRYSTAL, 2));
        tag("deepslate_thaumcraft_ore", 7, a(Aspect.EARTH, 1, Aspect.TRAP, 3, Aspect.CRYSTAL, 2));
    }
    private static void tag(String name, AspectList aspects) { tag(name, OreDictionary.WILDCARD_VALUE, aspects); }
    private static void tag(String name, int metadata, AspectList aspects) {
        Item item = GameRegistry.findItem("caveabyss", name);
        if (item != null) ThaumcraftApi.registerObjectTag(new ItemStack(item, 1, metadata), aspects);
    }
    private static AspectList a(Object... pairs) {
        AspectList aspects = new AspectList();
        for (int i = 0; i < pairs.length; i += 2) aspects.add((Aspect) pairs[i], (Integer) pairs[i + 1]);
        return aspects;
    }
}
