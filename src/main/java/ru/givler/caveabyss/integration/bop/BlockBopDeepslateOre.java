package ru.givler.caveabyss.integration.bop;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/** Seven overworld BOP gems, backed by their original gem drops. */
public final class BlockBopDeepslateOre extends Block {
    static final String[] GEMS = {
            "ruby", "peridot", "topaz", "tanzanite", "malachite", "sapphire", "amber"
    };
    @SideOnly(Side.CLIENT) private IIcon[] icons;

    public BlockBopDeepslateOre() {
        super(Material.rock);
        setBlockName("deepslate_bop_ore");
        setHardness(3.5F);
        setResistance(6.0F);
        setStepSound(soundTypeStone);
        setCreativeTab(CreativeTabs.tabBlock);
        for (int metadata = 0; metadata < GEMS.length; metadata++)
            setHarvestLevel("pickaxe", 2, metadata);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[GEMS.length];
        for (int i = 0; i < GEMS.length; i++)
            icons[i] = register.registerIcon("caveabyss:bop/deepslate_" + GEMS[i] + "_ore");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return icons[metadata >= 0 && metadata < icons.length ? metadata : 0];
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void getSubBlocks(Item item, CreativeTabs tab, List variants) {
        for (int i = 0; i < GEMS.length; i++) variants.add(new ItemStack(item, 1, i));
    }

    @Override
    public int damageDropped(int metadata) {
        return metadata;
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        Block original = BopIntegration.sourceOre();
        return original != null && metadata >= 0 && metadata < GEMS.length
                ? original.getDrops(world, x, y, z, (metadata + 1) * 2, fortune)
                : super.getDrops(world, x, y, z, metadata, fortune);
    }

    @Override
    public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        return true;
    }
}
