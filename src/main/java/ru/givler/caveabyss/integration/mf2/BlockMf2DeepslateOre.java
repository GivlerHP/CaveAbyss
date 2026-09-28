package ru.givler.caveabyss.integration.mf2;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import ru.givler.caveabyss.block.DeepOreDrops;

/** A deepslate host for an MF2 ore; MF2 still decides its drops and XP. */
public final class BlockMf2DeepslateOre extends Block {
    private final String oreName;

    public BlockMf2DeepslateOre(String oreName, String textureName, int harvestLevel,
                                float hardness, float resistance) {
        super(Material.rock);
        this.oreName = oreName;
        setBlockName("deepslate_mf2_" + textureName + "_ore");
        setHardness(hardness);
        setResistance(resistance);
        setStepSound(soundTypeStone);
        setCreativeTab(CreativeTabs.tabBlock);
        setHarvestLevel("pickaxe", harvestLevel);
        setBlockTextureName("caveabyss:mf2/deepslate_" + textureName + "_ore");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(getTextureName());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if ("oreMythic".equals(oreName)) {
            Block original = Mf2Integration.sourceOre(oreName);
            if (original != null) original.randomDisplayTick(world, x, y, z, random);
        }
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        Block original = Mf2Integration.sourceOre(oreName);
        return original != null ? DeepOreDrops.replaceSourceBlock(
                original.getDrops(world, x, y, z, 0, fortune), original, this, 0)
                : super.getDrops(world, x, y, z, metadata, fortune);
    }

    @Override
    public int getExpDrop(IBlockAccess world, int metadata, int fortune) {
        Block original = Mf2Integration.sourceOre(oreName);
        return original != null ? original.getExpDrop(world, 0, fortune) : 0;
    }

    @Override
    public boolean canSilkHarvest(World world, EntityPlayer player,
                                  int x, int y, int z, int metadata) {
        return true;
    }
}
