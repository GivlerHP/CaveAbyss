package ru.givler.caveabyss.block;

import java.util.ArrayList;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Preserve the source ore's resource drops, but keep block drops in deepslate. */
public final class DeepOreDrops {
    private DeepOreDrops() { }

    public static ArrayList<ItemStack> replaceSourceBlock(ArrayList<ItemStack> drops,
            Block source, Block deep, int deepMetadata) {
        Item sourceItem = Item.getItemFromBlock(source);
        if (sourceItem == null) return drops;
        for (int i = 0; i < drops.size(); i++) {
            ItemStack stack = drops.get(i);
            if (stack == null || stack.getItem() != sourceItem) continue;
            ItemStack replacement = new ItemStack(deep, stack.stackSize, deepMetadata);
            if (stack.hasTagCompound())
                replacement.setTagCompound((net.minecraft.nbt.NBTTagCompound) stack.getTagCompound().copy());
            drops.set(i, replacement);
        }
        return drops;
    }
}
