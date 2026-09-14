package com.example.geminiapi

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val REGION_NAME = "Hyderabad_Street_Offline_Final"

@Composable
fun RunScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var isDownloaded by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<String>() }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    fun addLog(msg: String) {
        logs.add(msg)
        Log.d("RunScreen", msg)
    }

    remember {
        addLog("Initializing MapLibre...")
        MapLibre.getInstance(context)
    }

    LaunchedEffect(Unit) {
        val offlineManager = OfflineManager.getInstance(context)
        offlineManager.setOfflineMapboxTileCountLimit(100000)

        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                val region = offlineRegions?.find { String(it.metadata).contains(REGION_NAME) }

                if (region != null) {
                    region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                        override fun onStatus(status: OfflineRegionStatus?) {
                            if (status?.isComplete == true) {
                                addLog("Map storage active.")
                                isDownloaded = true
                            } else {
                                addLog("Resuming setup...")
                                isDownloading = true
                                observeDownload(region, { downloadProgress = it }, {
                                    isDownloading = false
                                    isDownloaded = true
                                }, { addLog(it) })
                            }
                        }
                        override fun onError(error: String?) { addLog("ERR: $error") }
                    })
                } else {
                    addLog("Ready for setup.")
                }
            }
            override fun onError(error: String) { addLog("Storage ERR: $error") }
        })
    }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Run Feature", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                
                if (isDownloaded && mapInstance != null) {
                    Button(onClick = {
                        mapInstance?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(17.3850, 78.4867), 14.0))
                    }) {
                        Text("Reset")
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (!isDownloaded && !isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Hyderabad Map Setup", style = MaterialTheme.typography.headlineSmall)
                        Text("One-time download for street-level offline use.", modifier = Modifier.padding(16.dp))
                        Button(onClick = {
                            isDownloading = true
                            startNewDownload(context, { downloadProgress = it }, {
                                isDownloading = false
                                isDownloaded = true
                            }, { addLog(it) })
                        }) {
                            Text("Download Offline Map")
                        }
                    }
                } else if (isDownloading && !isDownloaded) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Saving Data to Phone...")
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                        )
                        Text("${(downloadProgress * 100).toInt()}% complete")
                    }
                } else {
                    MapViewContainer(
                        onLog = { addLog(it) },
                        onMapReady = { mapInstance = it }
                    )
                }
                
                // Debug UI
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp).align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.7f)).padding(4.dp)
                ) {
                    LazyColumn {
                        items(logs) { log ->
                            Text("> $log", color = Color.Green, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun startNewDownload(
    context: Context,
    onProgress: (Float) -> Unit,
    onComplete: () -> Unit,
    onLog: (String) -> Unit
) {
    val offlineManager = OfflineManager.getInstance(context)
    val bounds = LatLngBounds.Builder()
        .include(LatLng(17.48, 78.58))
        .include(LatLng(17.29, 78.39))
        .build()

    val definition = OfflineTilePyramidRegionDefinition(
        STYLE_URL, bounds, 11.0, 15.0, context.resources.displayMetrics.density
    )

    val metadata = JSONObject().apply {
        put("NAME", REGION_NAME)
    }.toString().toByteArray(Charsets.UTF_8)

    offlineManager.createOfflineRegion(definition, metadata, object : OfflineManager.CreateOfflineRegionCallback {
        override fun onCreate(offlineRegion: OfflineRegion) {
            onLog("Region created. Starting...")
            observeDownload(offlineRegion, onProgress, onComplete, onLog)
        }
        override fun onError(error: String) { onLog("Setup ERR: $error") }
    })
}

private fun observeDownload(
    region: OfflineRegion,
    onProgress: (Float) -> Unit,
    onComplete: () -> Unit,
    onLog: (String) -> Unit
) {
    region.setObserver(object : OfflineRegion.OfflineRegionObserver {
        override fun onStatusChanged(status: OfflineRegionStatus) {
            val progress = if (status.requiredResourceCount > 0) {
                status.completedResourceCount.toFloat() / status.requiredResourceCount.toFloat()
            } else 0f
            onProgress(progress)

            if (status.isComplete) {
                onLog("OFFLINE READY.")
                region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                onComplete()
            }
        }
        override fun onError(error: OfflineRegionError) { onLog("DL ERR: ${error.message}") }
        override fun mapboxTileCountLimitExceeded(limit: Long) { onLog("LIMIT EXCEEDED: $limit") }
    })
    region.setDownloadState(OfflineRegion.STATE_ACTIVE)
}

@Composable
fun MapViewContainer(onLog: (String) -> Unit, onMapReady: (MapLibreMap) -> Unit) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { view ->
        view.getMapAsync { map ->
            onMapReady(map)
            onLog("Loading cached data...")
            map.setStyle(STYLE_URL) {
                onLog("OFFLINE ACTIVE.")
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(17.3850, 78.4867), 13.0))
            }
        }
    }
}
