package com.measurepulse.ar.data.local

/**
 * First-launch sample projects so Home looks alive before the user's first scan.
 * Areas are computed from the footprint so every number on screen is internally consistent.
 */
object SampleScans {
    private const val DAY = 24L * 60 * 60 * 1000

    fun build(now: Long = System.currentTimeMillis()): List<RoomScanEntity> = listOf(
        scan(
            title = "Living Room 75\" TV Wall & Lounge", category = ScanCategory.FLOORPLAN,
            w = 5.80, l = 4.25, ceiling = 3.10, walls = 4,
            quality = "AR Verified ±1 cm", image = "bg_living_tv_wall", featured = true, createdAt = now,
        ),
        scan(
            title = "Master Suite & Terrace Scan", category = ScanCategory.FLOORPLAN,
            w = 4.60, l = 5.40, ceiling = 2.85, walls = 5,
            quality = "Mesh 99.8%", image = "bg_city_lounge", createdAt = now - DAY,
        ),
        scan(
            title = "Modern Studio & Kitchen", category = ScanCategory.AR_TAPE,
            w = 3.80, l = 4.85, ceiling = 2.70, walls = 4, detail = "Island Offset • 3 Plugs",
            quality = "Point-to-point ×12", image = "bg_ocean_room_scan", createdAt = now - 2 * DAY,
        ),
        scan(
            title = "Sky Terrace & Balcony", category = ScanCategory.FLOORPLAN,
            w = 3.20, l = 4.40, ceiling = 2.60, walls = 3, detail = "Glazing Run • 1.10 m Rail",
            quality = "PDF Exported", image = "bg_villa_pool", createdAt = now - 3 * DAY,
        ),
        scan(
            title = "Gallery Wall Frame Alignment", category = ScanCategory.LEVEL,
            w = 2.40, l = 0.12, ceiling = 1.45, walls = 1, detail = "3 Frames • 0.0° Level",
            quality = "Level ±0.1°", image = "bg_living_tv_wall", createdAt = now - 4 * DAY,
        ),
        scan(
            title = "Cliffside Villa Facade Height", category = ScanCategory.RANGEFINDER,
            w = 18.6, l = 9.40, ceiling = 9.65, walls = 2, detail = "Height 9.65 m • 28 m away",
            quality = "AI Rangefinder", image = "bg_villa_cliff", createdAt = now - 6 * DAY,
        ),
        scan(
            title = "Oak Shelf Thread & Bracket", category = ScanCategory.CALIPER,
            w = 0.92, l = 0.28, ceiling = 0.025, walls = 0, detail = "M8 × 1.25 Thread",
            quality = "Caliper 0.1 mm", image = "bg_city_lounge", createdAt = now - 8 * DAY,
        ),
        scan(
            title = "Oceanview Open-Plan Living", category = ScanCategory.FLOORPLAN,
            w = 9.20, l = 7.60, ceiling = 3.20, walls = 6,
            quality = "Mesh 99.4%", image = "bg_ocean_room_scan", createdAt = now - 10 * DAY,
        ),
    )

    private fun scan(
        title: String, category: ScanCategory, w: Double, l: Double, ceiling: Double, walls: Int,
        quality: String, image: String, detail: String? = null, featured: Boolean = false, createdAt: Long,
    ) = RoomScanEntity(
        title = title, category = category, widthM = w, lengthM = l, ceilingM = ceiling,
        areaSqM = w * l, wallCount = walls, detail = detail, qualityLabel = quality,
        imageKey = image, isFeatured = featured, createdAt = createdAt,
    )
}
