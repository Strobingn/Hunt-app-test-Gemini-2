package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.HuntingWaypoint
import com.example.data.model.LazDataset
import com.example.ui.theme.*

@Composable
fun AiTerrainDialog(
    dataset: LazDataset?,
    centerLat: Double,
    centerLng: Double,
    waypoints: List<HuntingWaypoint>,
    oracleBackendUrl: String,
    isAnalyzing: Boolean,
    analysisResult: String?,
    onUpdateBackendUrl: (String) -> Unit,
    onRunAnalysis: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var promptInput by remember { mutableStateOf("") }
    var showBackendConfig by remember { mutableStateOf(false) }
    var tempBackendUrl by remember { mutableStateOf(oracleBackendUrl) }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Assistant",
                            tint = LiDARGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "AI Terrain & Oracle Backend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (dataset != null) "Dataset: ${dataset.name}" else "Map View Analysis",
                                fontSize = 11.sp,
                                color = TextSecondaryLight
                            )
                        }
                    }

                    IconButton(onClick = { showBackendConfig = !showBackendConfig }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Backend Settings",
                            tint = if (oracleBackendUrl.isNotBlank()) LiDARGreen else HunterAmber
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Oracle Cloud Backend URL Configuration Box
                if (showBackendConfig) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Oracle Cloud / Custom Backend URL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = HunterAmber
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Enter your custom backend REST endpoint (e.g. http://140.238.xx.xx:8080/api/analyze). If empty, standard AI Studio Gemini model is used.",
                                fontSize = 10.sp,
                                color = TextSecondaryLight
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tempBackendUrl,
                                onValueChange = { tempBackendUrl = it },
                                label = { Text("http://<oracle-ip>:port/api") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onUpdateBackendUrl(tempBackendUrl)
                                    showBackendConfig = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestSagePrimary),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Save Endpoint")
                            }
                        }
                    }
                }

                // Quick Action Chips
                Text("Quick Analysis Options:", fontSize = 11.sp, color = Color.LightGray)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            promptInput = "Identify best tree stand locations and pinch points"
                            onRunAnalysis(promptInput)
                        },
                        label = { Text("Tree Stands", fontSize = 10.sp) },
                        leadingIcon = { Icon(Icons.Default.Place, null, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = {
                            promptInput = "Calculate thermal wind drafts for morning/evening"
                            onRunAnalysis(promptInput)
                        },
                        label = { Text("Thermals", fontSize = 10.sp) },
                        leadingIcon = { Icon(Icons.Default.Air, null, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = {
                            promptInput = "Suggest alignment calibration nudge values for this LAZ overlay"
                            onRunAnalysis(promptInput)
                        },
                        label = { Text("Alignment", fontSize = 10.sp) },
                        leadingIcon = { Icon(Icons.Default.Tune, null, modifier = Modifier.size(14.dp)) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Custom Prompt Input Field
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    label = { Text("Ask AI or Oracle Backend...") },
                    placeholder = { Text("e.g. What is the optimal stand direction for North wind?") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = { onRunAnalysis(promptInput.ifBlank { null }) },
                            enabled = !isAnalyzing
                        ) {
                            Icon(Icons.Default.Send, "Send", tint = ForestSageLight)
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                // Result Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = TacticalDarkBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        if (isAnalyzing) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                CircularProgressIndicator(color = LiDARGreen)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (oracleBackendUrl.isNotBlank()) "Connecting to Oracle Cloud Backend..." else "Querying Gemini AI Terrain Engine...",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                            }
                        } else if (!analysisResult.isNullOrBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = analysisResult,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    lineHeight = 18.sp
                                )
                            }
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "Tap a quick option above or type a prompt to query AI / Oracle Cloud backend for terrain insights.",
                                    fontSize = 12.sp,
                                    color = TextSecondaryLight
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { onRunAnalysis(null) },
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestSagePrimary)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Full Terrain Scan")
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}
