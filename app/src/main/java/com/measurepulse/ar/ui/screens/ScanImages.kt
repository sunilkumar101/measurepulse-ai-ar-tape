package com.measurepulse.ar.ui.screens

import com.measurepulse.ar.R

/** Maps a stored imageKey to a bundled drawable, or returns the key itself (file path/URI for captured photos). */
object ScanImages {
    private val bundled = mapOf(
        "bg_living_tv_wall" to R.drawable.bg_living_tv_wall,
        "bg_city_lounge" to R.drawable.bg_city_lounge,
        "bg_ocean_room_scan" to R.drawable.bg_ocean_room_scan,
        "bg_villa_pool" to R.drawable.bg_villa_pool,
        "bg_villa_cliff" to R.drawable.bg_villa_cliff,
    )

    fun model(imageKey: String): Any = bundled[imageKey] ?: imageKey
}
