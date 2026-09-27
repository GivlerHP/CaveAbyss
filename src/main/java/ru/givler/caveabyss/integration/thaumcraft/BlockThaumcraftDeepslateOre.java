package ru.givler.caveabyss.integration.thaumcraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import ru.givler.caveabyss.block.CaveBlocks;

/** Thaumcraft's eight ore variants with a deepslate appearance. */
public final class BlockThaumcraftDeepslateOre extends Block {
    public static final int[] INFUSED_COLORS = {
            0xFFFFFF, 0xFFFF7E, 0xFF3C01, 0x0090FF, 0x00A000, 0xEEDFFF, 0x5555F7
    };
    @SideOnly(Side.CLIENT) private IIcon cinnabarIcon;
    @SideOnly(Side.CLIENT) private IIcon infusedCracksIcon;
    @SideOnly(Side.CLIENT) private IIcon amberIcon;
    @SideOnly(Side.CLIENT) private static int renderId;

    @SideOnly(Side.CLIENT)
    public static void setRenderId(int id) {
        renderId = id;
    }

    @Override
    public int getRenderType() {
        return renderId;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    public BlockThaumcraftDeepslateOre() {
        super(Material.rock);
        setBlockName("deepslate_thaumcraft_ore");
        setHardness(4.5F);
        setResistance(6.0F);
        setStepSound(soundTypeStone);
        setCreativeTab(CreativeTabs.tabBlock);
        setHarvestLevel("pickaxe", 2, 0);
        setHarvestLevel("pickaxe", 2, 7);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        cinnabarIcon = register.registerIcon("caveabyss:thaumcraft/deepslate_cinnabar_ore");
        infusedCracksIcon = register.registerIcon("thaumcraft:infusedore");
        amberIcon = register.registerIcon("caveabyss:thaumcraft/amber_bearing_deepslate");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return metadata == 0 ? cinnabarIcon : metadata == 7 ? amberIcon
                : CaveBlocks.deepslate.getIcon(side, 0);
    }

    @SideOnly(Side.CLIENT)
    public IIcon getInfusedCracks() {
        return infusedCracksIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return getRenderColor(world.getBlockMetadata(x, y, z));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void getSubBlocks(Item item, CreativeTabs tab, List variants) {
        for (int metadata = 0; metadata <= 7; metadata++)
            variants.add(new ItemStack(item, 1, metadata));
    }

    @Override
    public int damageDropped(int metadata) {
        return metadata;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        Block source = ThaumcraftIntegration.sourceOre();
        return source != null ? source.getDrops(world, x, y, z, metadata, fortune)
                : super.getDrops(world, x, y, z, metadata, fortune);
    }

    @Override
    public int getExpDrop(IBlockAccess world, int metadata, int fortune) {
        Block source = ThaumcraftIntegration.sourceOre();
        return source != null ? source.getExpDrop(world, metadata, fortune) : 0;
    }

    @Override
    public boolean canSilkHarvest(World world, net.minecraft.entity.player.EntityPlayer player,
                                  int x, int y, int z, int metadata) {
        return true;
    }
}
