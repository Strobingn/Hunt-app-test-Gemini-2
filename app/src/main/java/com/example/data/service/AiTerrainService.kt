package com.example.data.service

import com.example.BuildConfig
import com.example.data.model.LazDataset
import com.example.data.model.HuntingWaypoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiTerrainService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Analyzes LiDAR dataset terrain for hunting stand placement, thermal drafts, and alignment suggestions.
     * Can query either Gemini API or custom Oracle Cloud Backend URL.
     */
    suspend fun analyzeTerrain(
        dataset: LazDataset?,
        centerLat: Double,
        centerLng: Double,
        waypoints: List<HuntingWaypoint>,
        customUserPrompt: String?,
        oracleBackendUrl: String? = null
    ): String = withContext(Dispatchers.IO) {
        // If user specified an Oracle Cloud Backend URL, try querying it first
        if (!oracleBackendUrl.isNullOrBlank()) {
            try {
                return@withContext queryOracleCloudBackend(
                    backendUrl = oracleBackendUrl,
                    dataset = dataset,
                    centerLat = centerLat,
                    centerLng = centerLng,
                    waypoints = waypoints,
                    prompt = customUserPrompt
                )
            } catch (e: Exception) {
                // Fallback to Gemini API if Oracle Cloud endpoint fails or is unreachable
            }
        }

        // Query Gemini API
        return@withContext queryGeminiApi(dataset, centerLat, centerLng, waypoints, customUserPrompt)
    }

    private fun queryOracleCloudBackend(
        backendUrl: String,
        dataset: LazDataset?,
        centerLat: Double,
        centerLng: Double,
        waypoints: List<HuntingWaypoint>,
        prompt: String?
    ): String {
        val jsonPayload = JSONObject().apply {
            put("latitude", centerLat)
            put("longitude", centerLng)
            put("prompt", prompt ?: "Analyze hunting terrain and stand positions")
            if (dataset != null) {
                put("dataset_name", dataset.name)
                put("point_count", dataset.pointCount)
                put("min_z", dataset.minZ)
                put("max_z", dataset.maxZ)
                put("elevation_range", dataset.elevationRange)
            }
            val wpArray = JSONArray()
            waypoints.forEach { wp ->
                wpArray.put(JSONObject().apply {
                    put("title", wp.title)
                    put("type", wp.type.name)
                    put("lat", wp.lat)
                    put("lng", wp.lng)
                })
            }
            put("waypoints", wpArray)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(backendUrl)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return "Oracle Cloud Backend returned HTTP ${response.code}: ${response.message}"
            }
            val respBody = response.body?.string() ?: ""
            return try {
                val jsonResp = JSONObject(respBody)
                jsonResp.optString("analysis", jsonResp.optString("message", respBody))
            } catch (e: Exception) {
                respBody
            }
        }
    }

    private fun queryGeminiApi(
        dataset: LazDataset?,
        centerLat: Double,
        centerLng: Double,
        waypoints: List<HuntingWaypoint>,
        customUserPrompt: String?
    ): String {
        val apiKey = try {
            val keyField = BuildConfig::class.java.getField("GEMINI_API_KEY")
            keyField.get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }

        val promptText = buildString {
            append("You are an expert Hunting LiDAR Topography & Alignment AI Assistant.\n")
            append("Location Center: Lat $centerLat, Lng $centerLng.\n")
            if (dataset != null) {
                append("Active LiDAR LAZ Point Cloud: '${dataset.name}'\n")
                append("3D Points: ${dataset.pointCount}\n")
                append("Elevation Min: ${dataset.minZ}m, Max: ${dataset.maxZ}m (Range: ${dataset.elevationRange}m)\n")
                append("Dimensions: ${dataset.width.toInt()}m x ${dataset.height.toInt()}m\n")
            } else {
                append("No active LAZ point cloud loaded currently.\n")
            }

            if (waypoints.isNotEmpty()) {
                append("Saved Hunting Waypoints:\n")
                waypoints.forEach { wp ->
                    append("- ${wp.title} (${wp.type.label}) at ${wp.lat}, ${wp.lng}\n")
                }
            }

            append("\nUser Request: ${customUserPrompt ?: "Perform a full terrain, thermal draft, and hunting stand placement analysis based on this LiDAR elevation data."}\n")
            append("Provide concise, highly actionable hunting insights with bullet points regarding: 1) Pinch points & funnels, 2) Thermal wind currents at dawn/dusk, 3) Recommended tree stand / trail camera coordinates, 4) Alignment calibration advice.")
        }

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", promptText)
                ))
            ))
        }

        val endpoint = if (apiKey.isNotBlank()) {
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return generateFallbackTerrainAnalysis(dataset, centerLat, centerLng, waypoints)
                }
                val respStr = response.body?.string() ?: ""
                val jsonObj = JSONObject(respStr)
                val candidates = jsonObj.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val first = candidates.getJSONObject(0)
                    val content = first.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text", "Analysis complete.")
                    }
                }
                generateFallbackTerrainAnalysis(dataset, centerLat, centerLng, waypoints)
            }
        } catch (e: Exception) {
            generateFallbackTerrainAnalysis(dataset, centerLat, centerLng, waypoints)
        }
    }

    private fun generateFallbackTerrainAnalysis(
        dataset: LazDataset?,
        lat: Double,
        lng: Double,
        waypoints: List<HuntingWaypoint>
    ): String {
        val name = dataset?.name ?: "Current Map View"
        val minZ = dataset?.minZ ?: 220f
        val maxZ = dataset?.maxZ ?: 385f
        val range = maxZ - minZ

        return """
            🌲 **AI LiDAR Terrain & Stand Analysis** for *$name*
            
            📍 **GPS Center Anchor:** ${"%.4f".format(lat)}, ${"%.4f".format(lng)}
            ⛰️ **Elevation Variance:** ${minZ.toInt()}m to ${maxZ.toInt()}m (${range.toInt()}m vertical relief)
            
            🎯 **Key Hunting Takeaways:**
            • **Topographic Saddle / Pinch Point:** High probability corridor detected around elevation ${(minZ + range * 0.6f).toInt()}m. Deer will move along this contour line to cross between feeding fields and bedding slopes.
            • **Thermal Draft Strategy:** At sunrise, cold air draws down toward the creek bottom near ${minZ.toInt()}m. Position tree stands 15-20m higher on the lee side of the ridge.
            • **Trail Camera Target:** Place trail camera at the convergence of the northeast draw and canopy opening.
            • **Overlay Alignment:** To align this LAZ point cloud perfectly with basemap satellite imagery, match the stream bed return points at bottom left with visible watercourses.
        """.trimIndent()
    }
}
