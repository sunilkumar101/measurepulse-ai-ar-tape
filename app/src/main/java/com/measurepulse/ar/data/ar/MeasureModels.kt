package com.measurepulse.ar.data.ar

enum class MeasureMode(val label: String) {
    SINGLE_LINE("Single Line"),
    ROOM_CHAIN("Room Chain"),
    BOX_FIT("3D Box Fit"),
    ANGLE("Angle °"),
}

/** Which measuring engine produced a frame. */
enum class EngineKind { ARCORE, SENSOR_FLOOR }

enum class TrackingStatus { INITIALIZING, SEARCHING, TRACKING, PAUSED }

/** Where the centre reticle's 3D point came from (drives the accuracy estimate). */
enum class HitSource(val label: String) {
    DEPTH("Depth"),
    PLANE("Plane"),
    FEATURE("Feature"),
    ESTIMATED_FLOOR("Est. floor"),
    GYRO_FLOOR("Gyro floor"),
    NONE("—"),
}

/** A locked measurement point. [accuracyM] is the 1-sigma error estimate at lock time. */
data class MeasurePoint(val id: Long, val position: Vec3, val accuracyM: Double)

/**
 * One camera frame's worth of spatial information from an engine.
 * Coordinates are in the engine's world frame; [up] tells which way is up.
 */
data class EngineFrame(
    val kind: EngineKind,
    val status: TrackingStatus,
    val reticle: Vec3?,
    val reticleAccuracyM: Double,
    val hitSource: HitSource,
    val up: Vec3,
    val cameraPosition: Vec3,
    val projector: ScreenProjector,
    /** Latest positions of ARCore anchors (they drift-correct over time). Empty for the sensor engine. */
    val anchoredPositions: Map<Long, Vec3> = emptyMap(),
    val hint: String? = null,
    val timestampMs: Long = System.currentTimeMillis(),
)

/** Furniture / TV footprint used by "3D Box Fit". Width = along the wall, depth = away from it. */
data class BoxItem(val name: String, val widthM: Double, val depthM: Double, val heightM: Double, val custom: Boolean = false)

object BoxPresets {
    val TV_55 = BoxItem("55\" 4K TV", 1.23, 0.08, 0.71)
    val TV_65 = BoxItem("65\" 4K TV", 1.45, 0.08, 0.83)
    val TV_75 = BoxItem("75\" 4K TV", 1.67, 0.09, 0.96)
    val TV_85 = BoxItem("85\" 4K TV", 1.89, 0.09, 1.09)
    val SOFA_3 = BoxItem("3-Seater Sofa", 2.10, 0.92, 0.85)
    val FRIDGE_DD = BoxItem("Double Door Fridge", 0.91, 0.74, 1.78)
    val BED_QUEEN = BoxItem("Queen Bed", 1.60, 2.03, 0.55)
    val BED_KING = BoxItem("King Bed", 1.93, 2.03, 0.55)
    val all = listOf(TV_55, TV_65, TV_75, TV_85, SOFA_3, FRIDGE_DD, BED_QUEEN, BED_KING)
}
