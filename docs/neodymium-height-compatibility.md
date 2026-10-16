# Neodymium height compatibility — 1.0.2.49

Neodymium Unofficial 0.4.3 stores 16 section visibility flags and 32 mesh slots.
Its WorldRenderer callbacks index these arrays with floorDiv(posY, 16), causing
index -4 for CaveAbyss's bottom section at Y=-64.

NeodymiumHeightTransformer patches the optional mod when its classes load:

- Allocate 20 visibility flags and 40 mesh slots.
- Map renderer section coordinates -4..15 to storage indices 0..19 in both
  onWorldRendererChanged and onWorldRendererPost. Mesh world coordinates remain
  signed and retain their original positions.
- Process all 20 sections in neoChunkChanged.
- Clear both render passes through putChunkMeshes on deletion, preserving its
  GPU cleanup, dirty flag and region mesh count. The original deletion code
  treated the section index as a single mesh index.
- Ignore temporary positions outside Y=-64..255 before looking up a NeoChunk.

Source review used the Neodymium checkout under
E:/Ariamis_src/FalseTweaks/NeodymiumUnofficial-0.4.3-unofficial. FalseTweaks's
NeoRenderer mixin does not overwrite these callbacks or their storage loop.
The fix is delivered in CaveAbyss; neither external JAR needs replacement.

Validation uses the installed Neodymium production JAR. ASM stack verification
covers both patched classes. NeodymiumHeightSmoke executes the patched array
constructor, callbacks, mesh insertion/removal and visibility loop with world
access and GPU operations stubbed. It covers 20 distinct section indices, both
passes, a partial sorting update, visible/invisible transitions, deletion and
region mesh counts, plus out-of-range renderer positions. JVM verification is
enabled for these tests. The existing development and production transform
suites also pass. Actual OpenGL rendering still requires an in-game check.

Version 1.0.2.49 also fixes mesh vertex placement. ChunkMesh applied
NeoRegion.toRelativeOffset to all three tessellator offsets. This wraps Y=-64
to Y=960 even though NeoRegion.RenderData.originY is always zero. The patch
preserves signed vertical offsets with D2F, retaining horizontal region wrapping.
NeodymiumVertexSmoke extracts the three actual offset expressions from the
installed patched class and runs the installed RenderUtilVanilla.polygonize
method. It verifies polygon coordinates at lower and upper section origins,
including fractional local coordinates, and checks negative/positive horizontal
region boundaries.
