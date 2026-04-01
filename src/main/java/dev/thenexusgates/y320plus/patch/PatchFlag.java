package dev.thenexusgates.y320plus.patch;

public enum PatchFlag {
    /** Replace sipush 320 → targetHeight (and ldc 320 as int). */
    HEIGHT,

    /** Replace sipush 319 → targetHeight − 1. */
    HEIGHT_M1,

    /** Replace bipush 10 → targetSectionCount. */
    SECTIONS,

    /** Replace ldc2_w 320.0d → (double) targetHeight. */
    HEIGHT_DOUBLE,

    /** Patch constant field values in ChunkUtil. */
    CHUNK_UTIL_FIELDS,

    /** Remove ACC_FINAL from ChunkColumn.sections field. */
    REMOVE_FINAL,

    /** Normalize BlockChunk.chunkSections array on entry/init. */
    NORM_BLOCK_CHUNK,

    /** Normalize ChunkColumn.sections and sectionHolders arrays. */
    NORM_CHUNK_COLUMN,

    /** Redirect ChunkStore.add() through safe-add with error recovery. */
    SAFE_ADD,

    /** Clamp chunkSections.length when building block packet. */
    CLAMP_BLOCK_PACKET,

    /** Clamp sections.length when building fluid packet. */
    CLAMP_FLUID_PACKET,

    /** Redirect getHeight → updateHeight in PositionProbeBase. */
    REDIRECT_HEIGHT,

    /** Replace probeMove result equality check with bitmask in MotionControllerWalk. */
    BITMASK_PROBE_MOVE,
}
