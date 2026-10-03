package ru.givler.caveabyss.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

public final class BlockCaveSpike extends Block {
    public static int renderId;
    public BlockCaveSpike() {
        super(Material.rock);
        setBlockName("cave_spike");
        setBlockTextureName("caveabyss:calcite");
        setHardness(1.5F);
        setStepSound(soundTypeStone);
    }
    @Override public boolean isOpaqueCube() { return false; }
    @Override public boolean renderAsNormalBlock() { return false; }
    @Override public int getRenderType() { return renderId; }
    @Override public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        float inset = (world.getBlockMetadata(x, y, z) & 3) * 0.105F + 0.06F;
        setBlockBounds(inset, 0, inset, 1 - inset, 1, 1 - inset);
    }
    @Override public void setBlockBoundsForItemRender() { setBlockBounds(0.25F, 0, 0.25F, 0.75F, 1, 0.75F); }
    @Override public net.minecraft.util.AxisAlignedBB getCollisionBoundingBoxFromPool(
            net.minecraft.world.World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }
}
