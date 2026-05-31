package com.example.ui.advanced

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import com.example.ui.webview.WebViewScreen

// NEW IMPORTS FOR INTUITIVE M3 DESIGN & PINCH-TO-ZOOM
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScreen(
    initialTab: String = ""
) {
    var selectedService by remember { mutableStateOf("ANM") }

    LaunchedEffect(initialTab) {
        if (initialTab.isNotEmpty()) {
            selectedService = initialTab
        }
    }

    val urls = mapOf(
        "Meteologix" to "https://meteologix.com/ro/model-charts/deu-hd/romania/significant-weather-extended.html",
        "UM" to "https://maps.meteo.pl/",
        "WXCharts" to "https://www.wxcharts.com/",
        "Windy" to "https://www.windy.com/",
        "GeoSphere" to "https://portale.geosphere.at/hpEUgw/index.php?p=HP_DUSTLOAD_EU&gl=EN"
    )

    val TabKeys = remember {
        val keys = mutableListOf("ANM")
        keys.addAll(urls.keys)
        keys.toList() + listOf("Info")
    }

    val activeTabIndex = remember(selectedService) {
        val idx = TabKeys.indexOf(selectedService)
        if (idx >= 0) idx else 0
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = activeTabIndex,
            edgePadding = 8.dp
        ) {
            TabKeys.forEach { title ->
                Tab(
                    selected = selectedService == title,
                    onClick = { selectedService = title },
                    text = { Text(title) }
                )
            }
        }
        
        if (selectedService == "Info") {
            AboutScreen()
        } else if (selectedService == "ANM") {
            AnmScreen()
        } else {
            val url = urls[selectedService]
            if (url != null) {
                WebViewScreen(url = url, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AboutScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val versionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val name = pInfo.versionName ?: "1.3.0"
            val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toString()
            }
            "$name (Build $code)"
        } catch (e: Exception) {
            "1.3.0 (Build 2)"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        
        // ROmeteo Branded Header/Logo
        Text(
            text = "🇷🇴 ⛈️",
            fontSize = 54.sp
        )
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            text = "ROmeteo",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Versiunea $versionName",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Description Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Aplicație meteorologică dedicată comunității din România.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Details List (using beautiful layouts)
        Text(
            text = "Informații Proiect",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )
        
        InfoRow(
            icon = Icons.Default.Info,
            title = "Versiune Aplicație",
            value = versionName
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        
        InfoRow(
            icon = Icons.Default.Star,
            title = "Autor & Credite",
            value = "David Marica"
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        
        InfoRow(
            icon = Icons.Default.Email,
            title = "Contact",
            value = "rometeotechnologies@gmail.com"
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        
        InfoRow(
            icon = Icons.Default.Cloud,
            title = "Surse Principale",
            value = "ICON-EU, ECMWF IFS HRES, GFS, Open-Meteo"
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        
        InfoRow(
            icon = Icons.Default.Code,
            title = "Cadru Dezvoltare",
            value = "Android Jetpack Compose"
        )
        
        Spacer(Modifier.height(24.dp))
        
        // Data licenses and attributions credit section
        Text(
            text = "Drepturi de Autor & Surse Date",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "• ICON-EU / Flash: Deutscher Wetterdienst (DWD)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• ECMWF IFS HRES: European Centre for Medium-Range Weather Forecasts",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• GFS: National Oceanic and Atmospheric Administration (NOAA)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Open-Meteo API: Sub licență CC BY 4.0",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Administrația Națională de Meteorologie (ANM)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Gherla info - stație Ecowitt",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Hărți avansate & Tehnologii satelit:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "  - Meteologix (Kachelmann Gruppe)\n" +
                           "  - MetDesk (WXCharts)\n" +
                           "  - Windy (Windyty, SE)\n" +
                           "  - ICM UW (Meteo.pl - Center for Mathematical and Computational Modelling, Univ. of Warsaw)\n" +
                           "  - Ecowitt (Rețeaua de stații meteorologice inteligente)\n" +
                           "  - GeoSphere Austria\n" +
                           "  - metmax (wrf-ro / METEO Technologies)",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Footer signature
        Text(
            text = "© 2026 David Marica - Toate drepturile rezervate",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ======================== ANM DATASET & COMPOSABLES ========================

data class AnmProduct(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String
)

val anmProducts = listOf(
    AnmProduct(
        id = "temp_orara",
        title = "Temperatură Orară",
        description = "Temperatura actuală a aerului măsurată la stațiile ANM din România",
        imageUrl = "https://www.meteoromania.ro/images/clima/temperatura_orara.png"
    ),
    AnmProduct(
        id = "temp_resimtita",
        title = "Temperatură Resimțită",
        description = "Temperatura resimțită de corpul uman luând în calcul viteza vântului și umezeala",
        imageUrl = "https://www.meteoromania.ro/images/clima/ttresim.png"
    ),
    AnmProduct(
        id = "confort_termic",
        title = "Indice Confort Termic",
        description = "Indicele de Confort Termic (ITU) calculat în timp real pe teritoriul țării",
        imageUrl = "https://www.meteoromania.ro/images/clima/ITU_orar_interpolat.png"
    ),
    AnmProduct(
        id = "temp_maxima",
        title = "Temperatură Maximă",
        description = "Temperatura maximă înregistrată pe parcursul zilei anterioare",
        imageUrl = "https://www.meteoromania.ro/images/clima/temperatura_orara_ieri.png"
    ),
    AnmProduct(
        id = "temp_minima",
        title = "Temperatură Minimă",
        description = "Temperatura minimă înregistrată în cursul nopții/dimineții anterioare",
        imageUrl = "https://www.meteoromania.ro/images/clima/temperatura_min_ieri.png"
    ),
    AnmProduct(
        id = "indice_temp_umezeala",
        title = "Indice Temp.-Umezeală",
        description = "Indicele temperatură-umezeală maxim realizat în cursul zilei de ieri",
        imageUrl = "https://www.meteoromania.ro/images/clima/ITU_orar_interpolat_ieri.png"
    ),
    AnmProduct(
        id = "precipitatii_totale",
        title = "Precipitații Totale",
        description = "Precipitațiile totale cumulate înregistrate în ultimele 24 de ore",
        imageUrl = "https://www.meteoromania.ro/images/clima/pp_daily.png"
    )
)

@Composable
fun AnmScreen() {
    var selectedProductIndex by remember { mutableStateOf(0) }
    val product = anmProducts[selectedProductIndex]
    
    // Increment key to force-refresh images dynamically 
    var refreshTrigger by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hărți Meteo ANM",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = "Selectează hărțile de la Administrația Națională de Meteorologie pentru monitorizare în timp real:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 4.dp, bottom = 12.dp)
        )
        
        // Scrollable row of selection chips
        ScrollableRowOfChips(
            selectedIndex = selectedProductIndex,
            onSelectedChange = { selectedProductIndex = it }
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Description Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = product.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Dynamic Zoomable image viewer
        ZoomableWeatherImageViewer(
            imageUrl = product.imageUrl,
            refreshTrigger = refreshTrigger,
            onRefresh = { refreshTrigger++ }
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "* Sugestie: Folosește două degete pentru zoom (pinch-to-zoom) și trage pentru a deplasa harta pe regiunea dorită.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ScrollableRowOfChips(
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(anmProducts.size) { index ->
            AnmChip(
                text = anmProducts[index].title,
                isSelected = index == selectedIndex,
                onClick = { onSelectedChange(index) }
            )
        }
    }
}

@Composable
fun AnmChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun ZoomableWeatherImageViewer(
    imageUrl: String,
    refreshTrigger: Int,
    onRefresh: () -> Unit
) {
    var showFullscreen by remember { mutableStateOf(false) }

    val cacheBustUrl = remember(imageUrl, refreshTrigger) {
        if (imageUrl.contains("?")) {
            "$imageUrl&t=${System.currentTimeMillis()}"
        } else {
            "$imageUrl?t=${System.currentTimeMillis()}"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clickable { showFullscreen = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = cacheBustUrl,
                contentDescription = "Previzualizare Hartă ANM",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )

            // Overlays: Refresh button + Instruction badge
            IconButton(
                onClick = { onRefresh() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizează",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Apasă pentru vizualizare interactivă (Pinch & Pan)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showFullscreen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                var scale by remember { mutableStateOf(1.2f) } // Initial sweet spot zoom
                var offset by remember { mutableStateOf(Offset.Zero) }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 6f)
                                    if (scale > 1f) {
                                        offset += pan
                                    } else {
                                        offset = Offset.Zero
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = cacheBustUrl,
                            contentDescription = "Hartă ANM Interactivă",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Top Action bar: Close (X) & Reset Zoom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.25f),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Reset Zoom", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { showFullscreen = false },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Refresh, // fallback or safe icon if Close is not imported, let's use standard default, wait, let's use a custom path or find an icon
                                contentDescription = "Închide",
                                tint = Color.White
                            )
                        }
                    }

                    // Bottom zoom controller bar (+ / - buttons)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                scale = (scale - 0.5f).coerceIn(1f, 6f)
                                if (scale == 1f) offset = Offset.Zero
                            },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Micșorează",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "${String.format("%.1f", scale)}x",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                scale = (scale + 0.5f).coerceIn(1f, 6f)
                            },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Mărește",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
