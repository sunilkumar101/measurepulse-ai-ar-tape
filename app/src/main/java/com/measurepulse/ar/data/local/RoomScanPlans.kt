package com.measurepulse.ar.data.local

import com.measurepulse.ar.domain.floorplan.FloorPlan
import com.measurepulse.ar.domain.floorplan.FloorPlanCodec

/** The editable floor plan of a saved scan: its Room Chain polygon, or a W×L rectangle for older/sample scans. */
fun RoomScanEntity.toFloorPlan(): FloorPlan {
    val corners = FloorPlanCodec.decodeCorners(polygonJson)
    val base = if (corners != null) FloorPlan(corners, ceilingM = ceilingM.coerceAtLeast(1.5))
    else FloorPlan.rectangle(widthM.coerceAtLeast(0.3), lengthM.coerceAtLeast(0.3), ceilingM.coerceAtLeast(1.5))
    return base.copy(openings = FloorPlanCodec.decodeOpenings(openingsJson).filter { it.wallIndex < base.corners.size })
}

/** Scans that describe a room (and so make sense for the advisor / blueprints). */
val RoomScanEntity.isRoom: Boolean get() = category == ScanCategory.FLOORPLAN || category == ScanCategory.AR_TAPE
