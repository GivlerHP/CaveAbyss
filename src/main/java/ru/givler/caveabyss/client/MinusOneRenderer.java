package ru.givler.caveabyss.client;

import ru.givler.caveabyss.data.MinusOneLayer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

/** Renders loaded negative layers within the client's configured view distance. */
public final class MinusOneRenderer {
    @SubscribeEvent
    public void render(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null || mc.renderViewEntity == null) return;
        double cameraX = mc.renderViewEntity.lastTickPosX + (mc.renderViewEntity.posX - mc.renderViewEntity.lastTickPosX) * event.partialTicks;
        double cameraY = mc.renderViewEntity.lastTickPosY + (mc.renderViewEntity.posY - mc.renderViewEntity.lastTickPosY) * event.partialTicks;
        double cameraZ = mc.renderViewEntity.lastTickPosZ + (mc.renderViewEntity.posZ - mc.renderViewEntity.lastTickPosZ) * event.partialTicks;
        int centerChunkX = (int)Math.floor(cameraX) >> 4;
        int centerChunkZ = (int)Math.floor(cameraZ) >> 4;
        int renderDistance = mc.gameSettings.renderDistanceChunks;
        RenderBlocks renderer = new RenderBlocks(world);
        Tessellator tessellator = Tessellator.instance;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            mc.entityRenderer.enableLightmap(event.partialTicks);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glColor4f(1, 1, 1, 1);
            tessellator.startDrawingQuads();
            tessellator.setTranslation(-cameraX, -cameraY, -cameraZ);
            for (int chunkX = centerChunkX - renderDistance; chunkX <= centerChunkX + renderDistance; chunkX++) {
                for (int chunkZ = centerChunkZ - renderDistance; chunkZ <= centerChunkZ + renderDistance; chunkZ++) {
                    if (!world.getChunkProvider().chunkExists(chunkX, chunkZ)) continue;
                    for (Map.Entry<Integer, Integer> entry : MinusOneLayer.visibleBlocks(world.getChunkFromChunkCoords(chunkX, chunkZ)).entrySet()) {
                        int index = entry.getKey();
                        int x = (chunkX << 4) | (index & 15);
                        int y = (index >> 8) - 64;
                        int z = (chunkZ << 4) | ((index >> 4) & 15);
                        Block block = Block.getBlockById(entry.getValue() & 65535);
                        if (block != null && block != Blocks.air) renderer.renderBlockByRenderType(block, x, y, z);
                    }
                }
            }
            tessellator.setTranslation(0, 0, 0);
            tessellator.draw();
        } finally {
            tessellator.setTranslation(0, 0, 0);
            mc.entityRenderer.disableLightmap(event.partialTicks);
            GL11.glPopAttrib();
        }
    }
}
