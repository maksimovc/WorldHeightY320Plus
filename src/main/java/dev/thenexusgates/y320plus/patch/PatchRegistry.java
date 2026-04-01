package dev.thenexusgates.y320plus.patch;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static dev.thenexusgates.y320plus.patch.PatchFlag.*;

public final class PatchRegistry {

    // ── Internal class names used by special-case patches ───────────────
    public static final String CHUNK_UTIL          = "com/hypixel/hytale/math/util/ChunkUtil";
    public static final String CHUNK_STORE         = "com/hypixel/hytale/server/core/universe/world/storage/ChunkStore";
    public static final String CHUNK_COLUMN        = "com/hypixel/hytale/server/core/universe/world/chunk/ChunkColumn";
    public static final String BLOCK_CHUNK         = "com/hypixel/hytale/server/core/universe/world/chunk/BlockChunk";
    public static final String BLOCK_CHUNK_LOAD    = "com/hypixel/hytale/server/core/universe/world/chunk/BlockChunk$LoadBlockChunkPacketSystem";
    public static final String FLUID_LOAD          = "com/hypixel/hytale/builtin/fluid/FluidSystems$LoadPacketGenerator";
    public static final String POSITION_PROBE_BASE = "com/hypixel/hytale/server/npc/util/PositionProbeBase";
    public static final String MOTION_CTRL_WALK    = "com/hypixel/hytale/server/npc/movement/controllers/MotionControllerWalk";

    // ── Component / type descriptors ────────────────────────────────────
    public static final String HOLDER       = "com/hypixel/hytale/component/Holder";
    public static final String REF          = "com/hypixel/hytale/component/Ref";
    public static final String BLOCK_SECTION = "com/hypixel/hytale/server/core/universe/world/chunk/section/BlockSection";
    public static final String COLLISION_MODULE = "com/hypixel/hytale/server/core/modules/collision/CollisionModule";

    public static final String HOLDER_DESC             = "L" + HOLDER + ";";
    public static final String HOLDER_ARRAY_DESC       = "[L" + HOLDER + ";";
    public static final String REF_DESC                = "L" + REF + ";";
    public static final String REF_ARRAY_DESC          = "[L" + REF + ";";
    public static final String BLOCK_SECTION_ARRAY_DESC = "[L" + BLOCK_SECTION + ";";

    // ── Synthetic method names ──────────────────────────────────────────
    public static final String NORMALIZE_SECTIONS          = "hyxin$normalizeChunkSections";
    public static final String NORMALIZE_SECTIONS_DESC     = "(" + BLOCK_SECTION_ARRAY_DESC + ")" + BLOCK_SECTION_ARRAY_DESC;
    public static final String NORMALIZE_COL_SECTIONS      = "hyxin$normalizeColumnSections";
    public static final String NORMALIZE_COL_SECTIONS_DESC = "(" + REF_ARRAY_DESC + ")" + REF_ARRAY_DESC;
    public static final String NORMALIZE_HOLDERS           = "hyxin$normalizeSectionHolders";
    public static final String NORMALIZE_HOLDERS_DESC      = "(" + HOLDER_ARRAY_DESC + ")" + HOLDER_ARRAY_DESC;
    public static final String SAFE_ADD_METHOD             = "hyxin$addChunkHolderSafely";
    public static final String SAFE_ADD_DESC               = "(L" + CHUNK_STORE + ";" + HOLDER_DESC + ")" + REF_DESC;

    public static final String BLOCK_CHUNK_4ARG_CTOR_DESC =
        "(Lcom/hypixel/hytale/server/core/universe/world/chunk/palette/ShortBytePalette;"
            + "Lcom/hypixel/hytale/server/core/universe/world/chunk/palette/IntBytePalette;"
            + "Lcom/hypixel/hytale/server/core/universe/world/chunk/environment/EnvironmentChunk;"
            + BLOCK_SECTION_ARRAY_DESC + ")V";

    public static final String VALIDATE_POSITION_DESC =
        "(Lcom/hypixel/hytale/server/core/universe/world/World;"
            + "Lcom/hypixel/hytale/math/shape/Box;"
            + "Lcom/hypixel/hytale/math/vector/Vector3d;"
            + "Lcom/hypixel/hytale/server/core/modules/collision/CollisionResult;)I";

    public static final String HOLDER_ARRAY_LENGTH_MSG =
        "EntityHolder start and length exceed array length!";

    // ── Registry ────────────────────────────────────────────────────────

    private static final Map<String, Set<PatchFlag>> REGISTRY;

    static {
        Map<String, Set<PatchFlag>> m = new HashMap<>(64);

        // ── A: Core chunk infrastructure ────────────────────────────────
        m.put(CHUNK_UTIL,   EnumSet.of(CHUNK_UTIL_FIELDS));
        m.put(BLOCK_CHUNK,  EnumSet.of(HEIGHT, SECTIONS, NORM_BLOCK_CHUNK));
        m.put(CHUNK_COLUMN, EnumSet.of(SECTIONS, NORM_CHUNK_COLUMN, REMOVE_FINAL));
        m.put(CHUNK_STORE,  EnumSet.of(SECTIONS, SAFE_ADD));
        m.put("com/hypixel/hytale/server/core/universe/world/chunk/WorldChunk", EnumSet.of(HEIGHT));

        // ── B: Network packets ──────────────────────────────────────────
        m.put(BLOCK_CHUNK_LOAD, EnumSet.of(CLAMP_BLOCK_PACKET));
        m.put(FLUID_LOAD,       EnumSet.of(CLAMP_FLUID_PACKET));
        m.put("com/hypixel/hytale/server/core/io/handlers/SetupPacketHandler",          EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/server/core/io/handlers/game/GamePacketHandler",      EnumSet.of(HEIGHT_M1));

        // ── C: Collision / physics ──────────────────────────────────────
        m.put("com/hypixel/hytale/server/core/modules/collision/CollisionConfig",       EnumSet.of(SECTIONS));
        m.put("com/hypixel/hytale/server/core/modules/collision/BlockDataProvider",     EnumSet.of(SECTIONS));
        m.put("com/hypixel/hytale/server/core/modules/collision/WorldUtil",             EnumSet.of(HEIGHT, HEIGHT_M1, HEIGHT_DOUBLE));

        // ── D: Movement / NPC ───────────────────────────────────────────
        m.put(MOTION_CTRL_WALK,    EnumSet.of(HEIGHT_DOUBLE, BITMASK_PROBE_MOVE));
        m.put(POSITION_PROBE_BASE, EnumSet.of(REDIRECT_HEIGHT));
        m.put("com/hypixel/hytale/server/npc/corecomponents/movement/BodyMotionFindWithTarget", EnumSet.of(HEIGHT_DOUBLE));
        m.put("com/hypixel/hytale/server/npc/blackboard/view/blocktype/BlockTypeView",          EnumSet.of(HEIGHT));

        // ── E: Block placement / targeting ──────────────────────────────
        m.put("com/hypixel/hytale/server/core/modules/interaction/BlockPlaceUtils",                                          EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/server/core/modules/interaction/interaction/config/client/PlaceBlockInteraction",          EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/server/core/modules/interaction/interaction/config/client/SimpleBlockInteraction",         EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/server/core/modules/interaction/interaction/config/server/RunOnBlockTypesInteraction",     EnumSet.of(HEIGHT_M1, SECTIONS));
        m.put("com/hypixel/hytale/server/core/util/TargetUtil",                                                              EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/server/core/prefab/selection/standard/BlockSelection",                                     EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/server/core/universe/world/accessor/IChunkAccessorSync",                                   EnumSet.of(HEIGHT));

        // ── F: World generation ─────────────────────────────────────────
        m.put("com/hypixel/hytale/server/core/universe/world/worldgen/GeneratedBlockChunk",            EnumSet.of(HEIGHT, SECTIONS));
        m.put("com/hypixel/hytale/server/worldgen/chunk/ChunkGenerator",                               EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/server/worldgen/chunk/ChunkGeneratorExecution",                      EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/builtin/hytalegenerator/engine/chunkgenerator/StagedChunkGenerator", EnumSet.of(HEIGHT, SECTIONS));
        m.put("com/hypixel/hytale/builtin/hytalegenerator/GridUtils",                                  EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/server/core/universe/world/worldgen/provider/FlatWorldGenProvider",  EnumSet.of(HEIGHT));

        // ── G: Notifications / block systems ────────────────────────────
        m.put("com/hypixel/hytale/server/core/universe/world/WorldNotificationHandler",                EnumSet.of(HEIGHT, HEIGHT_DOUBLE));
        m.put("com/hypixel/hytale/server/core/universe/world/connectedblocks/ConnectedBlocksUtil",     EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/server/core/blocktype/BlockTypeModule$FixFillerBlocksSystem",        EnumSet.of(HEIGHT, HEIGHT_M1, SECTIONS));
        m.put("com/hypixel/hytale/server/core/universe/world/chunk/environment/EnvironmentChunk$BulkWriter$ColumnWriter", EnumSet.of(HEIGHT_M1));

        // ── H: Spawning ─────────────────────────────────────────────────
        m.put("com/hypixel/hytale/server/spawning/SpawningContext",                                    EnumSet.of(HEIGHT, HEIGHT_M1, HEIGHT_DOUBLE));
        m.put("com/hypixel/hytale/server/spawning/world/system/ChunkSpawningSystems",                  EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/server/spawning/local/LocalSpawnControllerSystem",                   EnumSet.of(HEIGHT_M1));
        m.put("com/hypixel/hytale/server/spawning/util/FloodFillPositionSelector",                     EnumSet.of(HEIGHT_M1));

        // ── I: Gameplay ─────────────────────────────────────────────────
        m.put("com/hypixel/hytale/server/core/asset/type/fluid/FireFluidTicker",                       EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/builtin/adventure/farming/FarmingSystems",                           EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/builtin/adventure/farming/FarmingSystems$Ticking",                   EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/builtin/adventure/farming/config/modifiers/WaterGrowthModifierAsset", EnumSet.of(HEIGHT));
        m.put("com/hypixel/hytale/builtin/weather/components/WeatherTracker",                          EnumSet.of(HEIGHT_M1));
        m.put("com/hypixel/hytale/builtin/portals/ui/PortalSpawnFinder",                               EnumSet.of(HEIGHT_M1));

        REGISTRY = Collections.unmodifiableMap(m);
    }

    private PatchRegistry() {
    }

    public static Set<PatchFlag> flagsFor(String internalName) {
        return REGISTRY.getOrDefault(internalName, Set.of());
    }

    public static boolean isTarget(String internalName) {
        return REGISTRY.containsKey(internalName);
    }

    public static int targetCount() {
        return REGISTRY.size();
    }
}
