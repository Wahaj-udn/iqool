package com.example.geminiapi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONObject
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.offline.*
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.util.Locale

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val REGION_NAME = "Hyderabad_Street_Offline_Final"
private const val PATH_SOURCE_ID = "run-path-source"
private const val PATH_LAYER_ID = "run-path-layer"

@Composable
fun RunScreen(
    onBack: () -> Unit,
    locationViewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val locationState by locationViewModel.locationState.collectAsState()
    var isDownloaded by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<String>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            locationViewModel.startRunning()
        }
    }

    fun addLog(msg: String) {
        logs.add(msg)
        Log.d("RunScreen", msg)
    }

    LaunchedEffect(Unit) {
        try {
            addLog("Checking Storage...")
            val offlineManager = OfflineManager.getInstance(context)
            
            offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    val region = offlineRegions?.find { 
                        try { String(it.metadata).contains(REGION_NAME) } catch (e: Exception) { false }
                    }
                    if (region != null) {
                        region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                            override fun onStatus(status: OfflineRegionStatus?) {
                                if (status?.isComplete == true) {
                                    addLog("Offline data ready and done for..")
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
                            override fun onError(error: String?) { addLog("Status ERR: $error") }
                        })
                    } else {
                        addLog("No local data found.")
                    }
                }
                override fun onError(error: String) { addLog("List ERR: $error") }
            })
        } catch (e: Exception) {
            addLog("Init Error: ${e.localizedMessage}")
        }
    }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Run Recording", style = MaterialTheme.typography.titleMedium)
                
                Button(
                    onClick = {
                        if (locationState.isTracking) {
                            locationViewModel.stopRunning()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (locationState.isTracking) Color.Red else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (locationState.isTracking) "Stop Running" else "Start Running")
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (!isDownloaded && !isDownloading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Hyderabad Map Setup")
                        Button(onClick = {
                            isDownloading = true
                            startNewDownload(context, { downloadProgress = it }, {
                                isDownloading = false
                                isDownloaded = true
                            }, { addLog(it) })
                        }) {
                            Text("Setup Offline Map")
                        }
                    }
                } else if (isDownloading && !isDownloaded) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Downloading...")
                        LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    MapViewContainer(
                        locationState = locationState,
                        onLog = { addLog(it) }
                    )
                }
                
                locationState.location?.let {
                    Card(
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Lat: ${String.format(Locale.US, "%.5f", it.latitude)}", color = Color.White)
                            Text("Lon: ${String.format(Locale.US, "%.5f", it.longitude)}", color = Color.White)
                            Text("Acc: ${it.accuracy.toInt()}m", color = Color.White)
                            Text("Points: ${locationState.pathPoints.size}", color = Color.Cyan)
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f)).padding(4.dp)
                ) {
                    LazyColumn { items(logs) { Text(it, color = Color.Green, style = MaterialTheme.typography.bodySmall) } }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun MapViewContainer(locationState: LocationState, onLog: (String) -> Unit) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasInitialZoomed by remember { mutableStateOf(false) }

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
            if (map.style == null) {
                map.setStyle(STYLE_URL) { style ->
                    try {
                        val locationComponent = map.locationComponent
                        locationComponent.activateLocationComponent(
                            LocationComponentActivationOptions.builder(context, style).build()
                        )
                        locationComponent.isLocationComponentEnabled = true
                        locationComponent.renderMode = RenderMode.COMPASS
                        locationComponent.cameraMode = CameraMode.TRACKING
                        
                        // Setup Path Layer
                        val source = GeoJsonSource(PATH_SOURCE_ID)
                        style.addSource(source)
                        
                        val layer = LineLayer(PATH_LAYER_ID, PATH_SOURCE_ID)
                        layer.setProperties(
                            PropertyFactory.lineColor(Color.Blue.toArgb()),
                            PropertyFactory.lineWidth(6f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                        )
                        style.addLayer(layer)
                        
                        onLog("Map Engine Ready.")
                    } catch (e: Exception) { onLog("Setup Err: ${e.localizedMessage}") }
                }
            }
        }
    }

    // Dynamic path update
    LaunchedEffect(locationState.pathPoints) {
        if (locationState.pathPoints.size >= 2) {
            mapView.getMapAsync { map ->
                map.style?.let { style ->
                    val source = style.getSourceAs<GeoJsonSource>(PATH_SOURCE_ID)
                    val points = locationState.pathPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
                    source?.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(points)))
                }
            }
        }
    }

    LaunchedEffect(locationState.location) {
        locationState.location?.let { loc ->
            mapView.getMapAsync { map ->
                if (!hasInitialZoomed) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 17.0))
                    hasInitialZoomed = true
                } else {
                    map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(loc.latitude, loc.longitude)))
                }
            }
        }
    }
}

private fun startNewDownload(context: Context, onProgress: (Float) -> Unit, onComplete: () -> Unit, onLog: (String) -> Unit) {
    try {
        val offlineManager = OfflineManager.getInstance(context)
        val bounds = LatLngBounds.Builder().include(LatLng(17.55, 78.65)).include(LatLng(17.25, 78.30)).build()
        val definition = OfflineTilePyramidRegionDefinition(STYLE_URL, bounds, 10.0, 15.0, context.resources.displayMetrics.density)
        val metadata = JSONObject().apply { put("NAME", REGION_NAME) }.toString().toByteArray(Charsets.UTF_8)
        offlineManager.createOfflineRegion(definition, metadata, object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion) { observeDownload(offlineRegion, onProgress, onComplete, onLog) }
            override fun onError(error: String) { onLog("Setup ERR: $error") }
        })
    } catch (e: Exception) { onLog("DL ERR: ${e.localizedMessage}") }
}

private fun observeDownload(region: OfflineRegion, onProgress: (Float) -> Unit, onComplete: () -> Unit, onLog: (String) -> Unit) {
    region.setObserver(object : OfflineRegion.OfflineRegionObserver {
        override fun onStatusChanged(status: OfflineRegionStatus) {
            val progress = if (status.requiredResourceCount > 0) status.completedResourceCount.toFloat() / status.requiredResourceCount.toFloat() else 0f
            onProgress(progress)
            if (status.isComplete) { onLog("READY!"); region.setDownloadState(OfflineRegion.STATE_INACTIVE); onComplete() }
        }
        override fun onError(error: OfflineRegionError) { onLog("DL ERR: ${error.message}") }
        override fun mapboxTileCountLimitExceeded(limit: Long) { onLog("LIMIT!") }
    })
    region.setDownloadState(OfflineRegion.STATE_ACTIVE)
}
