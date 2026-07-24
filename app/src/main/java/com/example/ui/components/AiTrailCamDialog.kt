package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ml.TrailCamAiAnalyzer
import com.example.ml.TrailCamAnalysis
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiTrailCamDialog(
    onDismiss: () -> Unit
) {
    var selectedSample by remember { mutableStateOf("Whitetail Buck at Water Hole") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<TrailCamAnalysis?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TacticalDarkSurface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Trail Cam AI",
                        tint = HunterAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI Trail Cam Vision Classifier",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Automated Species & Antler Point Analysis",
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Select Sample Photo / Sensor Feed:", fontSize = 11.sp, color = Color.LightGray)
                Spacer(Modifier.height(6.dp))

                listOf(
                    "Whitetail Buck at Water Hole",
                    "Elk Herd Crossing Upper Saddle",
                    "Doe & Fawn Group in Drainage"
                ).forEach { sampleName ->
                    FilterChip(
                        selected = selectedSample == sampleName,
                        onClick = { selectedSample = sampleName },
                        label = { Text(sampleName, fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isAnalyzing = true
                            analysisResult = TrailCamAiAnalyzer.analyzeTrailCamPhoto(null, selectedSample)
                            isAnalyzing = false
                        }
                    },
                    enabled = !isAnalyzing,
                    colors = ButtonDefaults.buttonColors(containerColor = LiDARGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Running AI Vision...", color = Color.Black)
                    } else {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Analyze Camera Photo", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (analysisResult != null) {
                    val result = analysisResult!!
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = result.species,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = HunterAmber
                                )
                                Text(
                                    text = "${(result.confidence * 100).toInt()}% Match",
                                    fontSize = 11.sp,
                                    color = LiDARGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (result.estimatedAntlerPoints != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Antler Class: ${result.estimatedAntlerPoints}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Time: ${result.timeOfDay} | Count: ${result.count}",
                                fontSize = 11.sp,
                                color = TopoCyan
                            )

                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = result.details,
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}
