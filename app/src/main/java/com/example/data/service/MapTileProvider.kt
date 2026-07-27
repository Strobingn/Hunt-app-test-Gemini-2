package com.example.data.service

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ui.viewmodel.MapType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.*

/**
 * Asynchronous Map Tile Provider for Satellite, Hybrid, and Terrain imagery.
 * Converts GPS (lat, lng, zoom) into Web Mercator tiles (z/x/y) and fetches raster tile bitmaps.
 * Employs thread-safe LRU memory caching, debounced state invalidation, and fail-safe error handling.
 */
object MapTileProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // 64MB LRU ImageBitmap Cache
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceIn(16 * 1024, 64 * 1024)

    private val tileCache = object : LruCache<String, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: ImageBitmap): Int {
            return (bitmap.width * bitmap.height * 4 / 1024).coerceAtLeast(1)
        }
    }

    private val pendingRequests = ConcurrentHashMap<String, Boolean>()
    private val failedTiles = ConcurrentHashMap<String, Boolean>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _tileVersion = MutableStateFlow(0)
    val tileVersion: StateFlow<Int> = _tileVersion.asStateFlow()

    private var notifyJob: Job? = null

    private fun triggerRecomposition() {
        if (notifyJob?.isActive == true) return
        notifyJob = scope.launch(Dispatchers.Default) {
            delay(100) // Debounce 100ms to batch tile updates cleanly
            _tileVersion.value = _tileVersion.value + 1
        }
    }

    fun getTile(zoom: Int, tileX: Int, tileY: Int, mapType: MapType): ImageBitmap? {
        val cacheKey = "${mapType.name}_${zoom}_${tileX}_${tileY}"

        synchronized(tileCache) {
            val cached = tileCache.get(cacheKey)
            if (cached != null) return cached
        }

        if (pendingRequests[cacheKey] == true || failedTiles[cacheKey] == true) {
            return null
        }

        pendingRequests[cacheKey] = true

        scope.launch {
            try {
                val url = getTileUrl(mapType, zoom, tileX, tileY)
                if (url == null) {
                    failedTiles[cacheKey] = true
                    return@launch
                }

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 HuntAlign/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val stream: InputStream? = response.body?.byteStream()
                        if (stream != null) {
                            val bitmap = BitmapFactory.decodeStream(stream)
                            if (bitmap != null) {
                                val imageBmp = bitmap.asImageBitmap()
                                synchronized(tileCache) {
                                    tileCache.put(cacheKey, imageBmp)
                                }
                                triggerRecomposition()
                            } else {
                                failedTiles[cacheKey] = true
                            }
                        } else {
                            failedTiles[cacheKey] = true
                        }
                    } else {
                        failedTiles[cacheKey] = true
                    }
                }
            } catch (e: Exception) {
                failedTiles[cacheKey] = true
            } finally {
                pendingRequests.remove(cacheKey)
            }
        }

        return null
    }

    private fun getTileUrl(mapType: MapType, z: Int, x: Int, y: Int): String? {
        val maxTile = (1 shl z) - 1
        if (x < 0 || x > maxTile || y < 0 || y > maxTile) return null

        return when (mapType) {
            MapType.SATELLITE, MapType.HYBRID -> {
                "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x"
            }
            MapType.TERRAIN -> {
                "https://tile.opentopomap.org/$z/$x/$y.png"
            }
            MapType.DARK_VECTOR -> {
                "https://tile.openstreetmap.org/$z/$x/$y.png"
            }
        }
    }

    fun latLngToTileX(lat: Double, lng: Double, zoom: Int): Double {
        val clampedLng = lng.coerceIn(-180.0, 180.0)
        return (clampedLng + 180.0) / 360.0 * (1 shl zoom)
    }

    fun latLngToTileY(lat: Double, lng: Double, zoom: Int): Double {
        val latRad = Math.toRadians(lat.coerceIn(-85.05112878, 85.05112878))
        return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
    }
}
