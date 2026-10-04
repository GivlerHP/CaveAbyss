package ru.givler.caveabyss.integration.mf2;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import ru.givler.caveabyss.block.CaveBlocks;

/** Keep under-tier MF2 picks at the same mining speed as an empty hand. */
public final class DeepHarvestSpeed {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.entityPlayer.capabilities.isCreativeMode) return;
        Object name = Block.blockRegistry.getNameForObject(event.block);
        if (event.block != CaveBlocks.deepslate && (name == null
                || !name.toString().startsWith("caveabyss:deepslate_"))) return;
        ItemStack held = event.entityPlayer.getCurrentEquippedItem();
        int toolLevel = held == null ? -1 : held.getItem().getHarvestLevel(held, "pickaxe");
        if (toolLevel < event.block.getHarvestLevel(event.metadata))
            event.newSpeed = Math.min(event.newSpeed, 1.0F);
    }
}
