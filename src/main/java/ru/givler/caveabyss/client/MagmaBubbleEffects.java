package ru.givler.caveabyss.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityBubbleFX;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.audio.PositionedSoundRecord;
import ru.givler.caveabyss.block.CaveBlocks;
import ru.givler.caveabyss.handler.MagmaBubbleColumns;

/** Nearby particles and the original whirlpool sounds for magma bubble columns. */
public final class MagmaBubbleEffects {
    private boolean wasInsideColumn;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        WorldClient world = minecraft.theWorld;
        EntityPlayer player = minecraft.thePlayer;
        if (world == null || player == null) {
            wasInsideColumn = false;
            return;
        }
        if (minecraft.isGamePaused()) return;
        int px = MathHelper.floor_double(player.posX), py = MathHelper.floor_double(player.posY);
        int pz = MathHelper.floor_double(player.posZ);
        boolean insideColumn = MagmaBubbleColumns.isInColumn(world, px, py, pz);
        if (insideColumn) {
            if (!wasInsideColumn || world.getTotalWorldTime() % 45 == 0)
                minecraft.getSoundHandler().playSound(new PositionedSoundRecord(
                        new ResourceLocation("caveabyss", "bubble_column.whirlpool_inside"),
                        0.55F, 1.0F, (float)player.posX, (float)player.posY, (float)player.posZ));
            for (int i = 0; i < 3; i++)
                spawnDescendingBubble(minecraft, world, px + world.rand.nextDouble(),
                        py + world.rand.nextDouble(), pz + world.rand.nextDouble());
        }
        wasInsideColumn = insideColumn;
        if (world.getTotalWorldTime() % 6 != 0) return;
        int minY = world.provider.dimensionId == 0 ? -64 : 0;
        for (int x = px - 6; x <= px + 6; x++) for (int z = pz - 6; z <= pz + 6; z++) {
            if (!world.getChunkProvider().chunkExists(x >> 4, z >> 4)) continue;
            for (int y = Math.max(minY, py - 12); y <= Math.min(py + 3, 254); y++) {
                if (world.getBlock(x, y, z) != CaveBlocks.magma
                        || !MagmaBubbleColumns.isColumnWater(world, x, y + 1, z)) continue;
                for (int height = y + 1; height <= Math.min(y + 8, py + 8)
                        && MagmaBubbleColumns.isColumnWater(world, x, height, z); height++) {
                    spawnDescendingBubble(minecraft, world,
                            x + 0.2D + world.rand.nextDouble() * 0.6D,
                            height + world.rand.nextDouble(),
                            z + 0.2D + world.rand.nextDouble() * 0.6D);
                }
                if (world.getTotalWorldTime() % 90 == 0)
                    minecraft.getSoundHandler().playSound(new PositionedSoundRecord(
                            new ResourceLocation("caveabyss", "bubble_column.whirlpool_ambient"),
                            0.45F, 1.0F, x + 0.5F, y + 1.5F, z + 0.5F));
            }
        }
    }

    private static void spawnDescendingBubble(Minecraft minecraft, WorldClient world,
                                               double x, double y, double z) {
        EntityBubbleFX bubble = new EntityBubbleFX(world, x, y, z, 0.0D, 0.0D, 0.0D);
        bubble.motionY = -0.08D;
        minecraft.effectRenderer.addEffect(bubble);
    }
}
