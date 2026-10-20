import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import com.seibel.distanthorizons.common.render.openGl.terrain.OptifineVanillaChunkTracker;
import com.seibel.distanthorizons.common.wrappers.chunk.CaveAbyssCompat;
import makamys.neodymium.Neodymium;
import makamys.neodymium.renderer.*;

/** Compiles and exercises the production DH tracker against isolated world/GPU fixtures. */
public class DhHeightReadinessCheck {
    static final long KEY = OptifineVanillaChunkTracker.pack(0, 0);
    static void mask(boolean ready, int section, String label) {
        float opacity = OptifineVanillaChunkTracker.getLodOpacity(KEY, 1 << (section + 4));
        if ((opacity == 0) != ready) throw new AssertionError(label + ": " + opacity);
    }
    public static void main(String[] args) {
        WorldRenderer[] renderers = new WorldRenderer[20];
        if (com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper.getInclusiveMinBuildHeight(
                Minecraft.getMinecraft().theWorld.chunk) != -64
                || com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper.getHeight(
                        Minecraft.getMinecraft().theWorld.chunk) != 320)
            throw new AssertionError("DH chunk extraction does not include the full negative range");
        for (int i = 0; i < 20; i++) renderers[i] = new WorldRenderer(0, (i - 4) * 16, 0, true);
        OptifineVanillaChunkTracker.update(null);

        OptifineVanillaChunkTracker.update(renderers);
        if (!OptifineVanillaChunkTracker.isReady(KEY)) throw new AssertionError("Full 20-section column not ready");
        for (int section = -4; section < 16; section++) mask(true, section, "Distinct signed section");
        Minecraft.getMinecraft().theWorld.chunk.negativeReady = false;
        OptifineVanillaChunkTracker.update(renderers);
        for (int section = -4; section < 0; section++) mask(false, section, "Unsynchronized lower data must retain LOD");
        mask(true, 0, "Lower readiness must not alias upper section");
        Minecraft.getMinecraft().theWorld.chunk.negativeReady = true;
        renderers[0] = new WorldRenderer(0, -64, 0, false);
        OptifineVanillaChunkTracker.update(renderers);
        mask(false, -4, "Missing bottom mesh retains LOD");
        mask(true, -3, "Adjacent bottom mesh remains ready");
        Minecraft.getMinecraft().theWorld.chunk.negativeBlocks[0] = false;
        OptifineVanillaChunkTracker.update(renderers);
        mask(true, -4, "Confirmed empty negative section requires no mesh");
        Minecraft.getMinecraft().theWorld.chunk.negativeBlocks[0] = true;
        Neodymium.active = true;
        NeoRegion region = new NeoRegion();
        Neodymium.renderer.loadedRegionsMap.put(new net.minecraft.world.ChunkCoordIntPair(0, 0), region);
        region.chunk.chunkMeshes[8] = new Mesh(); // Signed section 0 resides at storage index 4.
        WorldRenderer[] upperOnly = {new WorldRenderer(0, 0, 0, true)};
        OptifineVanillaChunkTracker.update(upperOnly);
        mask(false, 0, "New GPU mesh waits for its render pass");
        OptifineVanillaChunkTracker.update(upperOnly);
        mask(true, 0, "Shifted Neodymium mesh lookup");
        region.chunk.chunkMeshes[0] = new Mesh();
        WorldRenderer[] bottomOnly = {new WorldRenderer(0, -64, 0, true)};
        OptifineVanillaChunkTracker.update(bottomOnly);
        OptifineVanillaChunkTracker.update(bottomOnly);
        mask(true, -4, "Bottom Neodymium mesh lookup");
        mask(false, 0, "Upper GPU mesh cannot make missing renderer ready");
        // Occlusion/frustum visibility must never resurrect a replacement LOD inside terrain.
        for (int i = 0; i < 20; i++) {
            Mesh mesh = new Mesh(); mesh.visible = false;
            region.chunk.chunkMeshes[i * 2] = mesh;
            renderers[i] = new WorldRenderer(0, (i - 4) * 16, 0, true);
            renderers[i].isVisible = false; renderers[i].isInFrustum = false;
        }
        OptifineVanillaChunkTracker.update(renderers);
        OptifineVanillaChunkTracker.update(renderers);
        for (int section = -4; section < 16; section++) mask(true, section, "Occluded GPU section must still hide its LOD");
        region.chunk.chunkMeshes[0].gpuStatus = Mesh.GPUStatus.UNSENT;
        OptifineVanillaChunkTracker.update(renderers);
        mask(false, -4, "Occluded but not uploaded mesh retains LOD");
        // Vanilla display lists can be ready without ever being drawn (occlusion query).
        Neodymium.active = false;
        OptifineVanillaChunkTracker.update(null);
        OptifineVanillaChunkTracker.update(renderers);
        for (int section = -4; section < 16; section++) mask(true, section, "Occluded vanilla section must hide its LOD without a draw call");
        Neodymium.active = true;
        region.chunk.chunkMeshes[0] = null;
        bottomOnly[0].skipRenderPass[0] = bottomOnly[0].skipRenderPass[1] = true;
        bottomOnly[0].needsUpdate = true;
        OptifineVanillaChunkTracker.update(bottomOnly);
        mask(false, -4, "Dirty empty mesh must not mask newly arrived terrain");
        Minecraft.getMinecraft().theWorld.chunk.worldObj.provider.dimensionId = -1;
        if (CaveAbyssCompat.minY(Minecraft.getMinecraft().theWorld.chunk.worldObj) != 0)
            throw new AssertionError("Other dimension height changed");
        if (!CaveAbyssCompat.isNegativeSectionEmpty(Minecraft.getMinecraft().theWorld.chunk, -4))
            throw new AssertionError("Non-extended dimension has phantom lower terrain");
        if (com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper.getHeight(
                Minecraft.getMinecraft().theWorld.chunk) != 256)
            throw new AssertionError("Non-extended chunk span changed");
        System.out.println("PASS: DH 20-section readiness, deferred snapshots, empty sections, shifted GPU slots, occlusion/frustum readiness and other dimensions");
    }
}
