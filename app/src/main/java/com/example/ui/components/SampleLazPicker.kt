package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.LazDataset
import com.example.data.model.SavedOverlay
import com.example.ui.theme.*

@Composable
fun DatasetAndPresetPickerDialog(
    sampleDatasets: List<LazDataset>,
    savedOverlays: List<SavedOverlay>,
    activeDatasetId: String?,
    onSelectDataset: (LazDataset) -> Unit,
    onSelectSavedOverlay: (SavedOverlay) -> Unit,
    onDeleteSavedOverlay: (SavedOverlay) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Datasets, 1 = Saved Presets

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = TacticalDarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select LiDAR LAZ Dataset / Preset",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = TacticalSurfaceVariant,
                    contentColor = ForestSageLight
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Sample Datasets (${sampleDatasets.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Saved Alignments (${savedOverlays.size})") }
                    )
                }

                Spacer(Modifier.height(12.dp))

                Box(modifier = Modifier.height(280.dp)) {
                    if (selectedTab == 0) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(sampleDatasets) { dataset ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (dataset.id == activeDatasetId) ForestSageContainer else TacticalSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectDataset(dataset)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Layers,
                                            contentDescription = null,
                                            tint = if (dataset.id == activeDatasetId) LiDARGreen else HunterAmber
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                dataset.name,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                dataset.description,
                                                fontSize = 11.sp,
                                                color = TextSecondaryLight
                                            )
                                            Text(
                                                "${dataset.pointCount} 3D Points | Elevation: ${dataset.minZ.toInt()}m - ${dataset.maxZ.toInt()}m",
                                                fontSize = 10.sp,
                                                color = TopoCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (savedOverlays.isEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    "No saved overlay alignment presets yet.\nUse the Save icon in the alignment panel to store custom positions.",
                                    color = TextSecondaryLight,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(savedOverlays) { saved ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = TacticalSurfaceVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSelectSavedOverlay(saved)
                                                onDismiss()
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(saved.name, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(
                                                    "GPS: ${"%.4f".format(saved.centerLat)}, ${"%.4f".format(saved.centerLng)}",
                                                    fontSize = 11.sp,
                                                    color = HunterAmber
                                                )
                                                Text(
                                                    "Scale: ${"%.2f".format(saved.scaleX)}x | Rot: ${saved.rotationDegrees.toInt()}°",
                                                    fontSize = 10.sp,
                                                    color = TextSecondaryLight
                                                )
                                            }
                                            IconButton(onClick = { onDeleteSavedOverlay(saved) }) {
                                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}
