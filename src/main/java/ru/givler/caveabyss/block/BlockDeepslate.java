package ru.givler.caveabyss.block;

import java.util.Random;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;

public final class BlockDeepslate extends Block {
    @SideOnly(Side.CLIENT)
    private IIcon topIcon;

    public BlockDeepslate() {
        super(Material.rock);
        setBlockName("deepslate");
        setBlockTextureName("caveabyss:deepslate");
        setHardness(3.0F);
        setResistance(6.0F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return Item.getItemFromBlock(CaveBlocks.cobbledDeepslate);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon("caveabyss:deepslate");
        topIcon = register.registerIcon("caveabyss:deepslate_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return side == 0 || side == 1 ? topIcon : blockIcon;
    }
}
