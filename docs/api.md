# CaveAbyss height and chunk API

Use normal Minecraft world coordinates everywhere. CaveAbyss extends the Overworld
down to Y=-64 while leaving the top at Y=255. World.getBlock, World.setBlock,
scheduled block updates, tile entities and neighbor notifications remain the
preferred block-level APIs.

Mods that need height or section information can compile against the CaveAbyss
deobfuscated JAR and use ru.givler.caveabyss.api:

    IDeepWorld deep = CaveAbyssAPI.world(world);
    if (deep.containsY(y)) {
        deep.setBlock(x, y, z, block, metadata, 3);
        deep.scheduleBlockUpdate(x, y, z, block, delay);
    }

    IDeepChunk chunk = deep.getChunk(x >> 4, z >> 4);
    IBlockSection belowZero = chunk.getSection(-1); // Y=-16..-1
    Block atMinusOne = belowZero.getBlock(x & 15, 15, z & 15);

IDeepChunk uses local X/Z and absolute block Y. IBlockSection uses local
X/Y/Z, with getSectionY() expressed in real sections (-4..15 in the
Overworld). Section views exist even when all blocks are air and are not
ExtendedBlockStorage instances. Writes through these views call
World.setBlock, preserving the normal callbacks and notification flags.

For read-only probes that must not load chunks, call
IDeepWorld.isChunkLoaded(chunkX, chunkZ) before getChunk. The API reports
Y=0..255 in dimensions without a CaveAbyss lower layer.

Existing mods using World and Chunk block methods work through the coremod
hooks. Code that directly indexes Chunk.getBlockStorageArray(), masks Y with
255, or rejects negative Y must be updated explicitly; this API cannot
intercept those operations.
