# World Height: Y320+

A Hytale server early plugin that removes the hardcoded 320-block world height limit,
allowing fully functional worlds up to **32,736 blocks tall** — the maximum the engine supports.

## What it does

Hytale's server has the world height hardcoded at 320 blocks (10 sections × 32 blocks)
across **43 server classes** — chunk storage, collision, physics, spawning,
world generation, NPC AI, block interactions, and more.

This plugin intercepts class loading before the server starts and rewrites every
hardcoded height constant to your configured value using ASM bytecode transformation.

### Patched systems (43 classes)

| Category | Classes | What's fixed |
|---|---|---|
| **Chunk storage** | ChunkUtil, ChunkStore, ChunkColumn, BlockChunk, WorldChunk | Section arrays resized, constant fields updated, safe deserialization |
| **Collision & physics** | CollisionConfig, BlockDataProvider, WorldUtil, MotionControllerWalk, PositionProbeBase | Mobs collide with blocks above Y=320, ground detection bitmask fix |
| **Network** | SetupPacketHandler, GamePacketHandler, BlockChunkLoadPacketSystem, FluidLoadPacketGenerator | Client receives correct world height, packet section clamping |
| **Interactions** | BlockPlaceUtils, PlaceBlockInteraction, SimpleBlockInteraction, RunOnBlockTypesInteraction, TargetUtil, BlockSelection, IChunkAccessorSync | Block placement and targeting work above Y=320 |
| **NPC / AI** | BodyMotionFindWithTarget, BlockTypeView | NPCs see and navigate blocks above Y=320 |
| **Spawning** | SpawningContext, ChunkSpawningSystems, LocalSpawnControllerSystem, FloodFillPositionSelector | Entities spawn correctly at any height |
| **Block systems** | WorldNotificationHandler, ConnectedBlocksUtil, FixFillerBlocksSystem, EnvironmentChunkColumnWriter | Block notifications, connected blocks, environment writing |
| **Gameplay** | FireFluidTicker, FarmingSystems, FarmingSystems$Ticking, WaterGrowthModifierAsset, WeatherTracker, PortalSpawnFinder | Fire, farming, weather, portals work at full height |
| **World generation** | GeneratedBlockChunk, ChunkGenerator, ChunkGeneratorExecution, StagedChunkGenerator, GridUtils, FlatWorldGenProvider | Terrain generates up to the new height limit |

## Installation

1. Build the plugin (see below) or download the release jar.
2. Copy `WorldHeightY320Plus-1.0.0.jar` into your server's `earlyplugins/` folder:
   ```
   <HytaleData>/UserData/Saves/<YourWorld>/earlyplugins/
   ```
3. Start the server. You'll see in the console:
   ```
   [Y320+] World height: 32736 blocks (1023 sections)
   [Y320+] Patched com.hypixel.hytale.math.util.ChunkUtil (1/43)
   ...
   ```

## Configuration

By default the plugin sets world height to **32,736 blocks** (1,023 sections) — the
engine maximum. If you want a lower height, set it via **any** of the following
(first found wins):

| Method | Example |
|---|---|
| JVM property | `-Dhytale.worldHeight=1024` |
| JVM property (alt) | `-Dhytale.world.height=1024` |
| Environment variable | `HYTALE_WORLD_HEIGHT=1024` |

### Constraints

- Must be **greater than 320** (the engine default).
- Must be a **multiple of 32** (auto-rounded up if not).
- Maximum: **32,736** — hard engine limit (`BlockChunk` stores heights as `short`
  via `ShortBytePalette`; max signed short = 32,767 → `⌊32767/32⌋ × 32 = 32,736`).

## Building from source

Requires **Java 25** (JDK 25 LTS).

```bash
cd WorldHeightY320Plus
./gradlew build
```

The built jar is at `build/libs/WorldHeightY320Plus-1.0.0.jar`.

## Technical details

- **Plugin type:** Hytale Early Plugin (`ClassTransformer`, priority 10000)
- **Bytecode library:** ASM 9.8 (bundled as fat jar)
- **Target runtime:** Java 25
- **Transformation approach:** ClassReader → HeightPatchClassVisitor → ClassWriter
- **Special patches:**
  - Array normalization for chunk sections (handles old-format 10-section data)
  - Safe deserialization with error-recovery round-trip
  - Packet section clamping for client compatibility
  - Height probe redirection (`getHeight` → `updateHeight`)
  - Collision result bitmask fix for extended ground detection

## Why 32,736 and not higher?

The Hytale engine stores per-block height values using `ShortBytePalette`, where
`getHeight()`, `setHeight()`, and `updateHeight()` all operate on Java `short`
(signed 16-bit: max 32,767). Any height value above 32,767 would overflow to negative,
corrupting the heightmap. The largest multiple of 32 (section size) that fits is
**32,736** (1,023 sections).

Increasing beyond this would require patching `ShortBytePalette` method signatures
from `short` to `int` throughout the engine — a much more invasive transformation
that risks breaking serialization formats.

## Compatibility

- **Hytale Server** — tested with the current release.
- **Other mods** — the plugin runs at priority 10000 (early) and only transforms
  known target classes, so it should be compatible with other mods.
- **Existing worlds** — worlds created with Y=320 will work. Chunks saved with
  10 sections are automatically expanded at load time.

## License

MIT
