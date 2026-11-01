package com.measurepulse.ar.data

import com.measurepulse.ar.data.local.RoomScanDao
import com.measurepulse.ar.data.local.RoomScanEntity
import com.measurepulse.ar.data.local.SampleScans
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomScanRepository @Inject constructor(
    private val dao: RoomScanDao,
) {
    private val seedLock = Mutex()

    fun observeScans(): Flow<List<RoomScanEntity>> = dao.observeAll()

    fun observeScan(id: Long): Flow<RoomScanEntity?> = dao.observeById(id)

    suspend fun toggleFavorite(id: Long) = dao.toggleFavorite(id)

    suspend fun latest() = dao.latest()
    suspend fun latestRoom() = dao.latestRoom()
    suspend fun byId(id: Long) = dao.byId(id)
    suspend fun delete(id: Long) = dao.delete(id)

    /** Persists an edited floor plan and keeps the summary columns (area, footprint, walls) in sync. */
    suspend fun savePlan(scan: RoomScanEntity, plan: com.measurepulse.ar.domain.floorplan.FloorPlan) {
        dao.update(
            scan.copy(
                polygonJson = com.measurepulse.ar.domain.floorplan.FloorPlanCodec.encodeCorners(plan.corners),
                openingsJson = com.measurepulse.ar.domain.floorplan.FloorPlanCodec.encodeOpenings(plan.openings),
                areaSqM = plan.areaSqM,
                widthM = plan.widthM,
                lengthM = plan.depthM,
                ceilingM = plan.ceilingM,
                wallCount = plan.corners.size,
            )
        )
    }

    /**
     * Saves a closed Room Chain as a floorplan project and returns its id (opened by Screen 4).
     * [floorPlan] is the 2D polygon in meters with the first corner at the origin.
     */
    suspend fun saveRoomChain(
        floorPlan: List<Pair<Double, Double>>,
        areaSqM: Double,
        accuracyM: Double,
        engineLabel: String,
        imageKey: String?,
        ceilingM: Double = DEFAULT_CEILING_M,
    ): Long {
        val xs = floorPlan.map { it.first }
        val ys = floorPlan.map { it.second }
        val json = floorPlan.joinToString(prefix = "[", postfix = "]") { (x, y) -> "[%.4f,%.4f]".format(java.util.Locale.US, x, y) }
        val date = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        return dao.insert(
            RoomScanEntity(
                title = "Room Scan · $date",
                category = com.measurepulse.ar.data.local.ScanCategory.FLOORPLAN,
                widthM = (xs.max() - xs.min()),
                lengthM = (ys.max() - ys.min()),
                ceilingM = ceilingM,
                areaSqM = areaSqM,
                wallCount = floorPlan.size,
                qualityLabel = "$engineLabel ±${"%.1f".format(java.util.Locale.US, accuracyM * 100)} cm",
                imageKey = imageKey ?: "bg_ocean_room_scan",
                polygonJson = json,
            )
        )
    }

    /** Saves a single-tool reading (rangefinder, caliper, level) as a project card. Returns its id. */
    suspend fun saveToolReading(
        title: String,
        category: com.measurepulse.ar.data.local.ScanCategory,
        detail: String,
        qualityLabel: String,
        imageKey: String,
        widthM: Double = 0.0,
        lengthM: Double = 0.0,
        heightM: Double = 0.0,
    ): Long = dao.insert(
        RoomScanEntity(
            title = title, category = category, widthM = widthM, lengthM = lengthM, ceilingM = heightM,
            areaSqM = widthM * lengthM, wallCount = 0, detail = detail, qualityLabel = qualityLabel, imageKey = imageKey,
        )
    )

    /** Inserts the sample projects once, on the very first launch (empty database). */
    suspend fun seedIfEmpty() = seedLock.withLock {
        if (dao.count() == 0) dao.insertAll(SampleScans.build())
    }

    companion object {
        const val DEFAULT_CEILING_M = 2.50
    }
}
