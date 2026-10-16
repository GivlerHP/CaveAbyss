# Negative height audit (2026-10-09)

Scope: Forge 1.7.10 development bytecode, vanilla block and tile-entity classes,
World/WorldServer power queries, ChunkCache and falling-block entity.

Confirmed fixes:
- Piston canExtend / tryExtend: lower limit follows dimension minimum.
- BlockFalling (sand, gravel, anvils): initial fall eligibility, instant fall loop,
  and placement after instant fall use the dimension minimum.
- Dragon egg: equivalent three fall-coordinate checks.
- EntityFallingBlock: timeout cleanup accepts negative heights instead of dropping
  the block merely because it is below Y=1.
- Beacon: pyramid levels below Y=0 are counted; sky visibility remains required.
- Generated static lava: mixing callback restored on chunk load.

Redstone:
- No hard Y=0 barrier found in wire strength calculation or World power queries.
- Native wire algorithm tested at Y=20, -1, -30 and -62, including strength
  15/14/13, indirect-power query, and decay to zero after switching power off.
- Test world supplies block storage directly; full client/server packet handling,
  lever interaction and repeater/comparator delays still need in-game validation.
- Repeater, comparator, torch, lever, button, pressure plate, powered rail,
  tripwire, dispenser/dropper and hopper coordinate reads use common World
  methods. No additional direct lower-height check found in the audited methods.

Already patched: negative World/Chunk reads and writes, metadata, tile entities,
ChunkCache reads, scheduled ticks' chunk-availability checks, random ticks.

Do not replace arbitrary numeric limits: inventory indices, metadata, signal
strength, fall timers and biome rules are not height bounds.

Verification: check / verifyCoreTransforms; CheckClassAdapter on real Forge
classes; RedstoneHeightSmoke. Changes outside the audited classes, especially
third-party mods, are not covered by this result.
