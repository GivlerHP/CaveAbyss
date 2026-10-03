package ru.givler.caveabyss.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;

public final class BlockAbyssStone extends Block {
    private final int tint;
    private final boolean boundary;

    public BlockAbyssStone(String name, String texture, int tint, boolean boundary) {
        super(Material.rock);
        this.tint = tint;
        this.boundary = boundary;
        setBlockName(name);
        setBlockTextureName(texture);
        setStepSound(soundTypeStone);
        if (boundary) { setBlockUnbreakable(); setResistance(6000000); disableStats(); }
        else { setHardness(2.0F); setResistance(6); }
    }

    @Override public int getBlockColor() { return tint; }
    @Override public int getRenderColor(int metadata) { return tint; }
    @Override public int colorMultiplier(IBlockAccess world, int x, int y, int z) { return tint; }
    @Override public int quantityDropped(Random random) { return boundary ? 0 : 1; }
    @Override public int getMobilityFlag() { return boundary ? 2 : 0; }
    @Override public boolean canEntityDestroy(IBlockAccess world, int x, int y, int z,
                                              net.minecraft.entity.Entity entity) {
        return !boundary && super.canEntityDestroy(world, x, y, z, entity);
    }
}
