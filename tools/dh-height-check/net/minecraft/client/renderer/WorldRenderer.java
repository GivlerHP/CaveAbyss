package net.minecraft.client.renderer;

/** Uses the runtime SRG name deliberately to catch incorrect reflection mappings. */
public class WorldRenderer {
    public int posX, posY, posZ;
    public boolean needsUpdate; public boolean[] skipRenderPass = new boolean[2]; public boolean isInFrustum=true,isVisible=true;
    private boolean field_78915_A;
    public WorldRenderer(int x, int y, int z, boolean ready) {
        posX = x; posY = y; posZ = z; field_78915_A = ready;
    }
}
