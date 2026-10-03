# Fractal Abyss

Select **Fractal Abyss** in the world type selector when creating a new world.
For a dedicated server use `level-type=abyss` with a new save folder.
Existing overworlds keep their existing generation and negative layers.

Each depth is a 256-block-high cave world with seven overlapping strata.
Minecraft's octave simplex noise warps corridor coordinates and shelf heights;
ridged slices cut irregular connections between shelves. Moisture controls moss,
ferns, mushrooms and hanging vines. Roots provide wood, while coal, iron and
diamonds occur in the rock. Calcite formations grow from floors and ceilings,
with magma in dry pockets. Bats, chickens, pigs and sheep populate
the caves without requiring another animal mod. No third-party assets are copied.
The new blocks reuse Minecraft textures and the mod's existing calcite texture.

Purple, unbreakable abyss bedrock seals the top and bottom. Every 192 blocks,
at X/Z coordinates `96 + 192*n`, there is a descent shaft and spiral path.
Entering its bottom below Y=12 transfers you to the next depth at the same shaft.
Climb its ladder above Y=244 to return to the previous depth. The first depth
has no upward exit. The first spawn is X=104, Y=225, Z=96.

New depths are allocated on demand, with no configured depth count. This is
an unbounded sequence of dimensions within Minecraft's integer IDs and available
storage, not a single world with infinitely negative Y. Transitions load another
dimension. Every depth retains its terrain and builds; players share the same
depth registry. Mounted players must dismount before transferring.

`data/CaveAbyssDepths.dat` stores the ordered dimension IDs. Keep this file with
the save's `DIM*` directories. Never change the dimension registry of an existing
save by hand. `caveabyss-world.cfg` configures the provider type ID (default 18645);
it must match on client and server and not conflict with another mod.
Clients receive registrations during login and before each transfer.

Transfer landings are searched for in existing terrain without deleting builds.
If a landing has been completely obstructed, the player is returned to the
source shaft ladder. Death uses the overworld/bed respawn rules.

Validation: `gradlew build` (includes all three verification tasks).
The geometry check covers seeds, distinct depths, layer density, boundaries,
negative coordinates, shafts and landing clearance. The terrain check uses
Minecraft's real block registry under LaunchClassLoader, generates 96 chunks,
checks decorations and dimension NBT, and writes a cross section to
`build/reports/abyss/terrain-section.png`. In-game verification should
also cover creation, descent, return, reconnect while deep, server restart,
two concurrent players and switching between saves/servers.

Local validation in this checkout uses `-I build/local-validation.init.gradle`
because the pinned `libs` directory was absent. The local override uses
MineFantasy II 2.11.2.4-dev, Thaumcraft 4.2.3.8-dev and CodeChickenCore 1.0.7.47
from adjacent projects, alongside the available BOP, Baubles and CodeChickenLib
jars. Production integration with the originally pinned versions is not verified
by this build. No client or server installation is modified by validation.
