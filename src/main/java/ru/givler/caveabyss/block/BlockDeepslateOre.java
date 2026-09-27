package ru.givler.caveabyss.block;

import java.util.ArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockOre;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/** Keeps the vanilla ore's drops and fortune behavior with a deepslate texture. */
public final class BlockDeepslateOre extends BlockOre {
    private final Block vanillaOre;

    public BlockDeepslateOre(String name, Block vanillaOre, int harvestLevel) {
        this.vanillaOre = vanillaOre;
        setBlockName(name);
        setBlockTextureName("caveabyss:" + name);
        setHardness(4.5F);
        setResistance(6.0F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", harvestLevel);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        return vanillaOre.getDrops(world, x, y, z, metadata, fortune);
    }

    @Override
    public int getExpDrop(IBlockAccess world, int metadata, int fortune) {
        return vanillaOre.getExpDrop(world, metadata, fortune);
    }
}
