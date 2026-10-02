package ru.givler.caveabyss.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;

/** Hot floor and the source of downward bubble columns in source water. */
public final class BlockMagma extends Block {
    public BlockMagma() {
        super(Material.rock);
        setBlockName("magma");
        setBlockTextureName("caveabyss:magma");
        setHardness(0.5F);
        setResistance(2.5F);
        setLightLevel(9.0F / 15.0F);
        setHarvestLevel("pickaxe", 0);
        setStepSound(soundTypeStone);
        setCreativeTab(CreativeTabs.tabBlock);
    }

    @Override
    public int getMixedBrightnessForBlock(IBlockAccess world, int x, int y, int z) {
        // The texture's dark stone stays dark, while its orange seams remain
        // emissive even below an ocean's unlit ceiling.
        return 0xF000F0;
    }

    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
        if (!world.isRemote && entity instanceof EntityLivingBase && !entity.isSneaking()
                && entity.ticksExisted % 10 == 0)
            entity.attackEntityFrom(DamageSource.onFire, 1.0F);
    }
}
