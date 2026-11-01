package com.measurepulse.ar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** What kind of tool produced a saved scan. Also drives the Home filter chips. */
enum class ScanCategory { AR_TAPE, FLOORPLAN, LEVEL, CALIPER, RANGEFINDER }

/**
 * A saved spatial scan / project. All lengths are stored in meters, areas in m², so the
 * Precision Unit Engine can show them in any unit system.
 */
@Entity(tableName = "room_scans")
data class RoomScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: ScanCategory,
    val widthM: Double,
    val lengthM: Double,
    val ceilingM: Double,
    val areaSqM: Double,
    val wallCount: Int,
    /** Short secondary detail, e.g. "Island Offset • 3 Plugs". Shown when not null. */
    val detail: String? = null,
    /** Mono accent footer, e.g. "Mesh 99.8%". */
    val qualityLabel: String,
    /** Name of a bundled drawable (e.g. "bg_living_tv_wall") or a file:// path for captured photos. */
    val imageKey: String,
    /** Room Chain floor polygon as JSON [[x,y],…] in meters (first point at origin). Null for non-room scans. */
    val polygonJson: String? = null,
    /** Doors & windows as JSON (see FloorPlanCodec). Null = none. */
    val openingsJson: String? = null,
    val isFeatured: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
