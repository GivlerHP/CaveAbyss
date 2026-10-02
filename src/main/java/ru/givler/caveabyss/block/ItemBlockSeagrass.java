package ru.givler.caveabyss.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public final class ItemBlockSeagrass extends ItemBlock {
    public ItemBlockSeagrass(Block block) {
        super(block);
        setHasSubtypes(true);
    }

    @Override public int getMetadata(int damage) { return 8 + (damage & 3); }
    @Override public String getUnlocalizedName(ItemStack stack) { return super.getUnlocalizedName(); }
}
