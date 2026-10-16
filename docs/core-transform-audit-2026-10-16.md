# Core transformer audit — 1.0.2.47

Reviewed method, field, owner and descriptor matching across all 16 registered
transformers. Production failures were caused by matching MCP names or class
owners directly before FML's deobfuscation pass.

Fixes:

- Resolve World/Chunk methods using the bytecode owner.
- Resolve invoked method names in the void fog and particle patches.
- Map Minecraft descriptors and owners in piston, falling block, entity packet,
  random tick, rendering, chunk tile entity and terrain generation patches.
- Accept SRG names for mineshaft population, dungeon wall blocks and bounding-box
  minimum Y. Correct updateLCG to field_73005_l and rawBufferIndex to field_147569_p.
- Support OptiFine's intermediate fog-factor local and WrUpdates renderer factory.
  Renderer coordinate matching follows the x/y/z argument order rather than
  assuming vanilla local-variable slots.

Validation:

- Existing development smoke suite, including bytecode verification and storage,
  world generation, random tick, redstone, spawn and integration checks.
- All 24 Minecraft target classes from Forge's actual binpatched production JAR,
  both before and after conversion to SRG names. Both outputs are mapped to MCP
  for ASM type verification against the development classpath.
- Five affected classes supplied by installed OptiFine E7: SpawnerAnimals,
  EntityRenderer, WorldClient, RenderGlobal and Tessellator. Patch counts and ASM
  stack verification pass.
- Installed BOP 2.1.0.2308 plant classes and RTG 1.1.1.7 provider: patch counts and
  ASM stack verification pass. CraftBukkit coverage uses synthetic fixtures;
  no actual Cauldron server is included in this client instance.

Run `gradlew.bat --offline build` for the development and production suites.
Supply `-PcompatMods=<instance .minecraft/mods directory>` to additionally test
the installed OptiFine E7, BOP and RTG versions listed above.

These checks do not start Minecraft or reproduce the full mod transformer chain.
An in-game launch and negative-height gameplay still require runtime validation.
