package ru.givler.caveabyss.integration.mf2;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.event.world.BlockEvent;
import ru.givler.caveabyss.block.BlockDeepslateOre;
import ru.givler.caveabyss.integration.bop.BlockBopDeepslateOre;
import ru.givler.caveabyss.integration.thaumcraft.BlockThaumcraftDeepslateOre;

/** MF2's placement guard has a fixed ore list, so it misses our deepslate variants. */
public final class DepletedDeepOreGuard {
    @SubscribeEvent
    public void onPlace(BlockEvent.PlaceEvent event) {
        if (event.player == null) return;
        ItemStack held = event.player.getCurrentEquippedItem();
        if (held == null || !isDeepOre(Block.getBlockFromItem(held.getItem()))) return;
        NBTTagCompound tag = held.getTagCompound();
        if (tag == null || !tag.getBoolean("depleted")) return;
        event.setCanceled(true);
        if (!event.world.isRemote)
            event.player.addChatMessage(new ChatComponentTranslation("caveabyss.depleted_ore"));
    }

    private static boolean isDeepOre(Block block) {
        return block instanceof BlockDeepslateOre
                || block instanceof BlockThaumcraftDeepslateOre
                || block instanceof BlockBopDeepslateOre
                || block instanceof BlockMf2DeepslateOre;
    }
}
