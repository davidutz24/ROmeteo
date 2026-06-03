package com.example.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToAdvanced: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    
    var showModelDropdown by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            getCurrentLocationAndFetchWeather(context, viewModel)
        } else {
            Toast.makeText(context, "Permisiunea de locație a fost refuzată", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                    placeholder = { Text("Caută oraș...") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) {
                                    getCurrentLocationAndFetchWeather(context, viewModel)
                                } else {
                                    launcher.launch(locationPermissions)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Locație GPS",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = viewModel::fetchWeather) {
                                Icon(Icons.Default.Search, contentDescription = "Căutare")
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            },
            actions = {
                IconButton(onClick = viewModel::fetchWeather) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualizează",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Box {
                    TextButton(onClick = { showModelDropdown = true }) {
                        Text(viewModel.availableModels.find { it.first == currentModel }?.second ?: "Model")
                    }
                    DropdownMenu(
                        expanded = showModelDropdown,
                        onDismissRequest = { showModelDropdown = false }
                    ) {
                        viewModel.availableModels.forEach { (modelId, modelName) ->
                            DropdownMenuItem(
                                text = { Text(modelName) },
                                onClick = {
                                    viewModel.onModelChange(modelId)
                                    showModelDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        )

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HomeUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is HomeUiState.Success -> {
                WeatherContent(state, viewModel, onNavigateToAdvanced)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherContent(
    state: HomeUiState.Success,
    viewModel: HomeViewModel,
    onNavigateToAdvanced: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val weather = state.weather
    val city = state.city
    val hourly = weather.hourlyData
    val daily = weather.dailyData
    
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    
    fun parseLocalDateTimeSafely(timeStr: String): LocalDateTime {
        if (timeStr.isBlank()) return LocalDateTime.now()
        return try {
            LocalDateTime.parse(timeStr, formatter)
        } catch (e: Exception) {
            try {
                LocalDateTime.parse(timeStr)
            } catch (e2: Exception) {
                try {
                    val trimmed = if (timeStr.length > 16) timeStr.substring(0, 16) else timeStr
                    LocalDateTime.parse(trimmed, formatter)
                } catch (e3: Exception) {
                    try {
                        LocalDate.parse(timeStr.substringBefore("T")).atStartOfDay()
                    } catch (e4: Exception) {
                        LocalDateTime.now()
                    }
                }
            }
        }
    }

    fun parseLocalDateSafely(dateStr: String): LocalDate {
        if (dateStr.isBlank()) return LocalDate.now()
        return try {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            try {
                LocalDate.parse(dateStr)
            } catch (e2: Exception) {
                try {
                    LocalDateTime.parse(dateStr).toLocalDate()
                } catch (e3: Exception) {
                    LocalDate.now()
                }
            }
        }
    }

    fun checkIfNight(dateTime: java.time.LocalDateTime): Boolean {
        val dateStr = dateTime.toLocalDate().toString() // "yyyy-MM-dd"
        val index = daily.time.indexOfFirst { it == dateStr }
        if (index != -1) {
            val sunriseStr = daily.sunrise.getOrNull(index)
            val sunsetStr = daily.sunset.getOrNull(index)
            if (!sunriseStr.isNullOrBlank() && !sunsetStr.isNullOrBlank()) {
                try {
                    val sunrise = parseLocalDateTimeSafely(sunriseStr)
                    val sunset = parseLocalDateTimeSafely(sunsetStr)
                    return dateTime.isBefore(sunrise) || dateTime.isAfter(sunset)
                } catch (e: Exception) {
                    // Fallback below
                }
            }
        }
        return dateTime.hour < 6 || dateTime.hour > 20
    }
    
    var currentIndex = 0
    for (i in hourly.time.indices) {
        val t = parseLocalDateTimeSafely(hourly.time[i])
        if (t.isAfter(now) || t.isEqual(now)) {
            currentIndex = i
            break
        }
    }

    val currentTemp = hourly.temperature2m.getOrNull(currentIndex) ?: 0.0
    val weatherCode = hourly.weatherCode.getOrNull(currentIndex) ?: 0
    val precipProb = hourly.precipitationProbability.getOrNull(currentIndex)?.toString()?.let { "$it%" } ?: "N/A"
    val windSpeed = hourly.windSpeed10m.getOrNull(currentIndex) ?: 0.0

    // Determine background color based on time and weather
    val isNight = checkIfNight(now)
    val targetBgColor = if (isNight) Color(0xFF1A1A2E) else Color(0xFF87CEEB)
    val bgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(1000))

    var selectedDate by remember { mutableStateOf(now.toLocalDate()) }
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgColor, MaterialTheme.colorScheme.background)))
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.fetchWeather() },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(Modifier.height(24.dp))
            
            // Modern Glassmorphic Current Weather Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNight) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.35f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isNight) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.45f),
                            if (isNight) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.1f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Draw atmospheric graphics on the background of the card
                    WeatherAtmosphereBackground(
                        code = weatherCode,
                        isNight = isNight,
                        modifier = Modifier.matchParentSize()
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = city.name,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNight) Color.White else Color(0xFF1E293B)
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WeatherIcon(
                                code = weatherCode,
                                isNight = isNight,
                                modifier = Modifier.size(88.dp)
                            )
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Text(
                                text = "${currentTemp.toInt()}°",
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Light,
                                color = if (isNight) Color.White else Color(0xFF0F172A)
                            )
                        }
                        
                        Spacer(Modifier.height(14.dp))
                        
                        // Modern, gorgeous visual capsule frame for the weather description text
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isNight) Color.White.copy(alpha = 0.08f) 
                                    else Color.Black.copy(alpha = 0.05f)
                                )
                                .border(
                                    1.dp,
                                    if (isNight) Color.White.copy(alpha = 0.15f) 
                                    else Color.Black.copy(alpha = 0.08f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = getWeatherDescription(weatherCode),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNight) Color.White else Color(0xFF1E293B)
                            )
                        }
                        
                        Spacer(Modifier.height(20.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Precipitation Badge
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isNight) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💧", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "$precipProb",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNight) Color(0xFFE2E8F0) else Color(0xFF475569)
                                )
                            }
                            
                            // Wind Badge
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isNight) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💨", fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${windSpeed.toInt()} km/h",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNight) Color(0xFFE2E8F0) else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(6.dp))
            val modelAttribution = when (state.currentModel) {
                "icon_eu" -> "Model: ICON-EU (© DWD)"
                "icon_seamless" -> "Model: ICON-EU Flash (© DWD)"
                "ecmwf_ifs025" -> "Model: ECMWF IFS HRES (© ECMWF)"
                "gfs_global" -> "Model: GFS 0.125° (© NOAA/NWS)"
                "gfs_seamless" -> "Model: GFS (© NOAA/NWS)"
                "meteoblue_seamless" -> "Model: Meteoblue (© meteoblue AG)"
                "open_meteo" -> "Model: Open-Meteo (© CC BY 4.0)"
                else -> "Model: ${state.currentModel.uppercase()}"
            }
            Text(
                text = modelAttribution,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.95f),
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Hourly Chart for selected date
            val dateStrPrefix = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val indicesForDate = hourly.time.indices.filter { hourly.time[it].startsWith(dateStrPrefix) }
            
            // Only show upcoming hours if it's today
            val displayIndices = if (selectedDate == now.toLocalDate()) {
                indicesForDate.filter { it >= currentIndex }
            } else {
                indicesForDate
            }

            if (displayIndices.isNotEmpty()) {
                Text("Prognoză Orară", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))
                
                var selectedHourIndex by remember { mutableStateOf<Int?>(null) }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(displayIndices.size) { i ->
                        val actualIndex = displayIndices[i]
                        val temp = hourly.temperature2m.getOrNull(actualIndex) ?: 0.0
                        val code = hourly.weatherCode.getOrNull(actualIndex) ?: 0
                        val precip = hourly.precipitationProbability.getOrNull(actualIndex)?.toString()?.let { "$it%" } ?: "N/A"
                        val wind = hourly.windSpeed10m.getOrNull(actualIndex) ?: 0.0
                        val fullTimeStr = hourly.time.getOrNull(actualIndex) ?: ""
                        val timeStr = fullTimeStr.substring(11, 16)
                        
                        val isHourNight = checkIfNight(parseLocalDateTimeSafely(fullTimeStr))
                        val isSelected = selectedHourIndex == actualIndex
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { selectedHourIndex = if (isSelected) null else actualIndex }
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                        ) {
                            Text(timeStr, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            Spacer(Modifier.height(8.dp))
                            WeatherIcon(
                                code = code,
                                isNight = isHourNight,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("${temp.toInt()}°", fontWeight = FontWeight.Bold)
                            
                            AnimatedVisibility(visible = isSelected) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Spacer(Modifier.height(8.dp))
                                    Text("💧 Precipitații: $precip", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("💨 Vânt: ${wind.toInt()} km/h", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(getWeatherDescription(code), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                Text("Nu există date orare", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }

            Spacer(Modifier.height(32.dp))

            Text("${daily.time.size} Zile (Selectează o zi)", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            
            // Daily Forecast (selectable)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(16.dp)
            ) {
                daily.time.forEachIndexed { index, dateStr ->
                    val date = parseLocalDateSafely(dateStr)
                    val isSelected = date == selectedDate
                    val max = daily.temperature2mMax.getOrNull(index)?.toInt() ?: 0
                    val min = daily.temperature2mMin.getOrNull(index)?.toInt() ?: 0
                    val code = daily.weatherCode.getOrNull(index) ?: 0
                    val dayOfWeek = if (date == now.toLocalDate()) "Azi" else {
                        when (date.dayOfWeek) {
                            java.time.DayOfWeek.MONDAY -> "Lun"
                            java.time.DayOfWeek.TUESDAY -> "Mar"
                            java.time.DayOfWeek.WEDNESDAY -> "Mie"
                            java.time.DayOfWeek.THURSDAY -> "Joi"
                            java.time.DayOfWeek.FRIDAY -> "Vin"
                            java.time.DayOfWeek.SATURDAY -> "Sâm"
                            java.time.DayOfWeek.SUNDAY -> "Dum"
                            else -> date.dayOfWeek.name.take(3)
                        }
                    }
                    
                    val bgAlpha by animateFloatAsState(if (isSelected) 0.3f else 0f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha))
                            .clickable { selectedDate = date }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dayOfWeek, modifier = Modifier.weight(1f), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            WeatherIcon(code = code, modifier = Modifier.size(28.dp))
                        }
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                            Text("$min°", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Spacer(Modifier.width(16.dp))
                            Text("$max°", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            Text("Stație Ecowitt", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                com.example.ui.webview.WebViewScreen(url = "https://www.ecowitt.net/home/share?authorize=74DBXT&device_id=UUN0RjJrRTUrbGkzRzdnemRyMlVmQT09")
            }
            
            Spacer(Modifier.height(24.dp))

            Text("Temperatură Orară România (ANM)", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    coil.compose.AsyncImage(
                        model = "https://www.meteoromania.ro/images/clima/temperatura_orara.png",
                        contentDescription = "Temperatură Orară România",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Sursă: Administrația Națională de Meteorologie (ANM)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.90f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info button redirecting to advanced menu
                TextButton(
                    onClick = onNavigateToAdvanced,
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Despre ROmeteo",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Info", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Credits on the right
                Text(
                    text = "© 2026 David Marica - ROmeteo",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}
}

private fun getCurrentLocationAndFetchWeather(context: Context, viewModel: HomeViewModel) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        Toast.makeText(context, "Serviciul de locație nu este disponibil", Toast.LENGTH_SHORT).show()
        return
    }
    
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    
    if (!hasFine && !hasCoarse) {
        Toast.makeText(context, "Permisiunea de locație lipsește", Toast.LENGTH_SHORT).show()
        return
    }
    
    var isGpsEnabled = false
    var isNetworkEnabled = false
    try {
        isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    } catch (e: Exception) {
        // provider not supported or restricted
    }
    try {
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (e: Exception) {
        // provider not supported or restricted
    }
    
    val providers = mutableListOf<String>()
    if (isGpsEnabled) providers.add(LocationManager.GPS_PROVIDER)
    if (isNetworkEnabled) providers.add(LocationManager.NETWORK_PROVIDER)
    
    var location: Location? = null
    
    for (provider in providers) {
        try {
            val loc = locationManager.getLastKnownLocation(provider)
            if (loc != null) {
                if (location == null || loc.accuracy < location.accuracy) {
                    location = loc
                }
            }
        } catch (e: SecurityException) {
            // Ignored
        } catch (e: Exception) {
            // Ignored
        }
    }
    
    if (location != null) {
        viewModel.fetchWeatherForCoordinates(location.latitude, location.longitude)
    } else {
        try {
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    viewModel.fetchWeatherForCoordinates(loc.latitude, loc.longitude)
                    try {
                        locationManager.removeUpdates(this)
                    } catch (e: Exception) {
                        // Ignored
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            
            val bestProvider = if (isNetworkEnabled) {
                LocationManager.NETWORK_PROVIDER
            } else if (isGpsEnabled) {
                LocationManager.GPS_PROVIDER
            } else {
                null
            }
            
            if (bestProvider != null) {
                locationManager.requestSingleUpdate(bestProvider, listener, Looper.getMainLooper())
                Toast.makeText(context, "Se obține locația curentă...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Vă rugăm să activați locația (GPS) din setările telefonului", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Eroare de securitate la obținerea locației", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Nu s-a putut obține locația", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun getStarPath(center: Offset, size: Float): Path {
    return Path().apply {
        moveTo(center.x, center.y - size)
        quadraticTo(center.x, center.y, center.x + size, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + size)
        quadraticTo(center.x, center.y, center.x - size, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - size)
        close()
    }
}

@Composable
fun WeatherIcon(
    code: Int,
    isNight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_anim")
    
    // Sun rays rotation
    val sunAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_angle"
    )
    
    // Cloud drift offset
    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_offset"
    )
    
    // Rain falling offset
    val rainOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_offset"
    )

    // Snow falling/wobble offset
    val snowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_offset"
    )

    // Lightning flicker alpha
    val lightningAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lightning_alpha"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        when (code) {
            0 -> { // Clear sky (Sun or Moon)
                if (isNight) {
                    // Draw glowing aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFEF08A).copy(alpha = 0.25f), Color.Transparent),
                            center = Offset(w * 0.52f, h * 0.48f),
                            radius = w * 0.48f
                        ),
                        radius = w * 0.48f,
                        center = Offset(w * 0.52f, h * 0.48f)
                    )
                    
                    val moonOuter = Path().apply {
                        addOval(Rect(center = Offset(w * 0.52f, h * 0.48f), radius = w * 0.32f))
                    }
                    val moonInner = Path().apply {
                        addOval(Rect(center = Offset(w * 0.40f, h * 0.40f), radius = w * 0.32f))
                    }
                    val crescentPath = Path.combine(
                        operation = PathOperation.Difference,
                        path1 = moonOuter,
                        path2 = moonInner
                    )
                    
                    drawPath(
                        path = crescentPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFEF08A), Color(0xFFF1F5F9)),
                            start = Offset(w * 0.3f, h * 0.3f),
                            end = Offset(w * 0.7f, h * 0.7f)
                        )
                    )
                    
                    val starPath1 = getStarPath(Offset(w * 0.25f, h * 0.32f), w * 0.08f)
                    val starPath2 = getStarPath(Offset(w * 0.72f, h * 0.62f), w * 0.05f)
                    
                    drawPath(
                        path = starPath1,
                        color = Color(0xFFFEF08A).copy(alpha = 0.9f)
                    )
                    drawPath(
                        path = starPath2,
                        color = Color(0xFFFEF08A).copy(alpha = 0.75f)
                    )
                } else {
                    val sunColor1 = Color(0xFFFFD54F)
                    val sunColor2 = Color(0xFFFF8F00)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(sunColor1, sunColor2),
                            center = Offset(w * 0.5f, h * 0.5f),
                            radius = w * 0.35f
                        ),
                        radius = w * 0.35f,
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                    // Sun rays
                    val rayCount = 8
                    val innerRadius = w * 0.40f
                    val outerRadius = w * 0.50f
                    val rayWidth = w * 0.06f
                    
                    for (i in 0 until rayCount) {
                        val angle = ((i * (360f / rayCount)) + sunAngle) * (Math.PI / 180f)
                        val startX = (w * 0.5f + Math.cos(angle) * innerRadius).toFloat()
                        val startY = (h * 0.5f + Math.sin(angle) * innerRadius).toFloat()
                        val endX = (w * 0.5f + Math.cos(angle) * outerRadius).toFloat()
                        val endY = (h * 0.5f + Math.sin(angle) * outerRadius).toFloat()
                        
                        drawLine(
                            color = Color(0xFFFF8F00),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = rayWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            1, 2 -> { // Partly Cloudy (Sun/Moon + Cloud)
                if (isNight) {
                    val moonOuter = Path().apply {
                        addOval(Rect(center = Offset(w * 0.65f, h * 0.35f), radius = w * 0.23f))
                    }
                    val moonInner = Path().apply {
                        addOval(Rect(center = Offset(w * 0.56f, h * 0.29f), radius = w * 0.23f))
                    }
                    val crescentPath = Path.combine(
                        operation = PathOperation.Difference,
                        path1 = moonOuter,
                        path2 = moonInner
                    )
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFEF08A).copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(w * 0.65f, h * 0.35f),
                            radius = w * 0.35f
                        ),
                        radius = w * 0.35f,
                        center = Offset(w * 0.65f, h * 0.35f)
                    )
                    
                    drawPath(
                        path = crescentPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFEF08A), Color(0xFFE2E8F0)),
                            start = Offset(w * 0.5f, h * 0.2f),
                            end = Offset(w * 0.8f, h * 0.5f)
                        )
                    )
                    
                    val starPath = getStarPath(Offset(w * 0.42f, h * 0.24f), w * 0.05f)
                    drawPath(
                        path = starPath,
                        color = Color(0xFFFEF08A).copy(alpha = 0.85f)
                    )
                } else {
                    // Draw a smaller sun first with rotating rays
                    val sunColor1 = Color(0xFFFFD54F)
                    val sunColor2 = Color(0xFFFF8F00)
                    val sunCenter = Offset(w * 0.65f, h * 0.35f)
                    val innerRadius = w * 0.28f
                    val outerRadius = w * 0.35f
                    val rayWidth = w * 0.04f
                    val rayCount = 8

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(sunColor1, sunColor2),
                            center = sunCenter,
                            radius = w * 0.25f
                        ),
                        radius = w * 0.25f,
                        center = sunCenter
                    )

                    for (i in 0 until rayCount) {
                        val angle = ((i * (360f / rayCount)) + sunAngle) * (Math.PI / 180f)
                        val startX = (sunCenter.x + Math.cos(angle) * innerRadius).toFloat()
                        val startY = (sunCenter.y + Math.sin(angle) * innerRadius).toFloat()
                        val endX = (sunCenter.x + Math.cos(angle) * outerRadius).toFloat()
                        val endY = (sunCenter.y + Math.sin(angle) * outerRadius).toFloat()
                        
                        drawLine(
                            color = Color(0xFFFF8F00),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = rayWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
                
                // Draw Cloud in front with drifting animation
                val cloudPath = Path().apply {
                    val nx = w * 0.15f
                    val ny = h * 0.45f
                    moveTo(nx + w * 0.15f, ny + h * 0.25f)
                    lineTo(nx + w * 0.55f, ny + h * 0.25f)
                    cubicTo(nx + w * 0.65f, ny + h * 0.25f, nx + w * 0.65f, ny + h * 0.08f, nx + w * 0.55f, ny + h * 0.08f)
                    cubicTo(nx + w * 0.52f, ny - h * 0.08f, nx + w * 0.35f, ny - h * 0.08f, nx + w * 0.37f, ny + h * 0.04f)
                    cubicTo(nx + w * 0.27f, ny - h * 0.05f, nx + w * 0.12f, ny + h * 0.03f, nx + w * 0.17f, ny + h * 0.14f)
                    cubicTo(nx + w * 0.12f, ny + h * 0.19f, nx + w * 0.12f, ny + h * 0.25f, nx + w * 0.15f, ny + h * 0.25f)
                    close()
                }
                
                translate(left = cloudOffset) {
                    drawPath(
                        path = cloudPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFF8FAFC), Color(0xFFCBD5E1)),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                }
            }
            3 -> { // Cloudy (Overlapping Clouds)
                // Back cloud slowly drifting
                val backCloud = Path().apply {
                    val nx = w * 0.28f
                    val ny = h * 0.22f
                    moveTo(nx + w * 0.15f, ny + h * 0.25f)
                    lineTo(nx + w * 0.5f, ny + h * 0.25f)
                    cubicTo(nx + w * 0.6f, ny + h * 0.25f, nx + w * 0.6f, ny + h * 0.08f, nx + w * 0.5f, ny + h * 0.08f)
                    cubicTo(nx + w * 0.48f, ny - h * 0.08f, nx + w * 0.32f, ny - h * 0.08f, nx + w * 0.34f, ny + h * 0.04f)
                    cubicTo(nx + w * 0.25f, ny - h * 0.05f, nx + w * 0.12f, ny + h * 0.03f, nx + w * 0.16f, ny + h * 0.14f)
                    cubicTo(nx + w * 0.12f, ny + h * 0.19f, nx + w * 0.12f, ny + h * 0.25f, nx + w * 0.15f, ny + h * 0.25f)
                    close()
                }
                translate(left = -cloudOffset * 0.4f) {
                    drawPath(
                        path = backCloud,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF94A3B8), Color(0xFF64748B)),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                }
                
                // Front cloud
                val frontCloud = Path().apply {
                    val nx = w * 0.05f
                    val ny = h * 0.35f
                    moveTo(nx + w * 0.15f, ny + h * 0.25f)
                    lineTo(nx + w * 0.55f, ny + h * 0.25f)
                    cubicTo(nx + w * 0.65f, ny + h * 0.25f, nx + w * 0.65f, ny + h * 0.08f, nx + w * 0.55f, ny + h * 0.08f)
                    cubicTo(nx + w * 0.52f, ny - h * 0.08f, nx + w * 0.35f, ny - h * 0.08f, nx + w * 0.37f, ny + h * 0.04f)
                    cubicTo(nx + w * 0.27f, ny - h * 0.05f, nx + w * 0.12f, ny + h * 0.03f, nx + w * 0.17f, ny + h * 0.14f)
                    cubicTo(nx + w * 0.12f, ny + h * 0.19f, nx + w * 0.12f, ny + h * 0.25f, nx + w * 0.15f, ny + h * 0.25f)
                    close()
                }
                translate(left = cloudOffset) {
                    drawPath(
                        path = frontCloud,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                }
            }
            45, 48 -> { // Fog (Clouds + horizontal lines)
                val fCloud = Path().apply {
                    val nx = w * 0.15f
                    val ny = h * 0.40f
                    moveTo(nx + w * 0.15f, ny + h * 0.25f)
                    lineTo(nx + w * 0.55f, ny + h * 0.25f)
                    cubicTo(nx + w * 0.65f, ny + h * 0.25f, nx + w * 0.65f, ny + h * 0.08f, nx + w * 0.55f, ny + h * 0.08f)
                    cubicTo(nx + w * 0.52f, ny - h * 0.08f, nx + w * 0.35f, ny - h * 0.08f, nx + w * 0.37f, ny + h * 0.04f)
                    cubicTo(nx + w * 0.27f, ny - h * 0.05f, nx + w * 0.12f, ny + h * 0.03f, nx + w * 0.17f, ny + h * 0.14f)
                    cubicTo(nx + w * 0.12f, ny + h * 0.19f, nx + w * 0.12f, ny + h * 0.25f, nx + w * 0.15f, ny + h * 0.25f)
                    close()
                }
                translate(left = cloudOffset * 0.3f) {
                    drawPath(
                        path = fCloud,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8)),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                }
                
                // Fog lines drifting side-to-side
                translate(left = -cloudOffset * 0.7f) {
                    val strokeW = w * 0.05f
                    drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(w * 0.2f, h * 0.72f),
                        end = Offset(w * 0.8f, h * 0.72f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(w * 0.3f, h * 0.82f),
                        end = Offset(w * 0.7f, h * 0.82f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> { // Rain (Cloud + raindrops)
                val rCloud = Path().apply {
                    val nx = w * 0.15f
                    val ny = h * 0.15f
                    moveTo(nx + w * 0.15f, ny + h * 0.25f)
                    lineTo(nx + w * 0.55f, ny + h * 0.25f)
                    cubicTo(nx + w * 0.65f, ny + h * 0.25f, nx + w * 0.65f, ny + h * 0.08f, nx + w * 0.55f, ny + h * 0.08f)
                    cubicTo(nx + w * 0.52f, ny - h * 0.08f, nx + w * 0.35f, ny - h * 0.08f, nx + w * 0.37f, ny + h * 0.04f)
                    cubicTo(nx + w * 0.27f, ny - h * 0.05f, nx + w * 0.12f, ny + h * 0.03f, nx + w * 0.17f, ny + h * 0.14f)
                    cubicTo(nx + w * 0.12f, ny + h * 0.19f, nx + w * 0.12f, ny + h * 0.25f, nx + w * 0.15f, ny + h * 0.25f)
                    close()
                }
                translate(left = cloudOffset * 0.2f) {
                    drawPath(
                        path = rCloud,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF64748B), Color(0xFF334155)),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                }
                
                // Raindrops falling using rainOffset
                val rWidth = w * 0.04f
                val rainColor = Color(0xFF0EA5E9)
                val rainLines = listOf(
                    Offset(w * 0.35f, h * 0.52f) to Offset(w * 0.30f, h * 0.68f),
                    Offset(w * 0.50f, h * 0.52f) to Offset(w * 0.45f, h * 0.68f),
                    Offset(w * 0.65f, h * 0.52f) to Offset(w * 0.60f, h * 0.68f),
                    Offset(w * 0.42f, h * 0.70f) to Offset(w * 0.37f, h * 0.85f),
                    Offset(w * 0.58f, h * 0.70f) to Offset(w * 0.53f, h * 0.85f)
                )
                // Drifts down and left
                translate(left = -rainOffset * 0.3f, top = rainOffset) {
                    rainLines.forEach { (start, end) ->
                        drawLine(
                            color = rainColor,
                            start = start,
                            end = end,
                            strokeWidth = rWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            71, 73, 75, 77, 85, 86 -> { // Snow (Cloud + snowflakes)
                val sCloud = Path().apply {
                    val nx = w * 0.15f
                    val ny = h * 0.15f
                    moveTo(nx + w * 0.15f, ny + h * 0.25f)
                    lineTo(nx + w * 0.55f, ny + h * 0.25f)
                    cubicTo(nx + w * 0.65f, ny + h * 0.25f, nx + w * 0.65f, ny + h * 0.08f, nx + w * 0.55f, ny + h * 0.08f)
                    cubicTo(nx + w * 0.52f, ny - h * 0.08f, nx + w * 0.35f, ny - h * 0.08f, nx + w * 0.37f, ny + h * 0.04f)
                    cubicTo(nx + w * 0.27f, ny - h * 0.05f, nx + w * 0.12f, ny + h * 0.03f, nx + w * 0.17f, ny + h * 0.14f)
                    cubicTo(nx + w * 0.12f, ny + h * 0.19f, nx + w * 0.12f, ny + h * 0.25f, nx + w * 0.15f, ny + h * 0.25f)
                    close()
                }
                translate(left = cloudOffset * 0.2f) {
                    drawPath(
                        path = sCloud,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8)),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                }
                
                // Snowflakes falling using snowOffset and swaying with cloudOffset
                val snowPoints = listOf(
                    Offset(w * 0.35f, h * 0.55f),
                    Offset(w * 0.52f, h * 0.60f),
                    Offset(w * 0.68f, h * 0.53f),
                    Offset(w * 0.42f, h * 0.75f),
                    Offset(w * 0.60f, h * 0.73f)
                )
                translate(left = cloudOffset * 0.5f, top = snowOffset) {
                    snowPoints.forEach { pt ->
                        drawCircle(
                            color = Color(0xFF7DD3FC),
                            radius = w * 0.04f,
                            center = pt
                        )
                    }
                }
            }
            95, 96, 99 -> { // Thunderstorm (Dark cloud + Lightning)
                val tCloud = Path().apply {
                    val nx = w * 0.15f
                    val ny = h * 0.15f
                    moveTo(nx + w * 0.15f, ny + h * 0.25f)
                    lineTo(nx + w * 0.55f, ny + h * 0.25f)
                    cubicTo(nx + w * 0.65f, ny + h * 0.25f, nx + w * 0.65f, ny + h * 0.08f, nx + w * 0.55f, ny + h * 0.08f)
                    cubicTo(nx + w * 0.52f, ny - h * 0.08f, nx + w * 0.35f, ny - h * 0.08f, nx + w * 0.37f, ny + h * 0.04f)
                    cubicTo(nx + w * 0.27f, ny - h * 0.05f, nx + w * 0.12f, ny + h * 0.03f, nx + w * 0.17f, ny + h * 0.14f)
                    cubicTo(nx + w * 0.12f, ny + h * 0.19f, nx + w * 0.12f, ny + h * 0.25f, nx + w * 0.15f, ny + h * 0.25f)
                    close()
                }
                translate(left = cloudOffset * 0.2f) {
                    drawPath(
                        path = tCloud,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                }
                // Lightning bolt path
                val lightningPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.42f)
                    lineTo(w * 0.42f, h * 0.62f)
                    lineTo(w * 0.52f, h * 0.62f)
                    lineTo(w * 0.45f, h * 0.85f)
                    lineTo(w * 0.6f, h * 0.55f)
                    lineTo(w * 0.5f, h * 0.55f)
                    close()
                }
                drawPath(
                    path = lightningPath,
                    color = Color(0xFFFBBF24).copy(alpha = lightningAlpha)
                )
            }
            else -> { // Unknown or basic
                drawCircle(
                    color = Color.LightGray,
                    radius = w * 0.3f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
        }
    }
}

@Composable
fun WeatherAtmosphereBackground(
    code: Int,
    isNight: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )

    val cloudDrift by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_drift"
    )

    val rainOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_offset"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val accentColor = if (isNight) Color(0xFF38BDF8) else Color(0xFFFFB300)
        
        when (code) {
            0 -> { // Clear (Glowing Sun in corner)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = (if (isNight) 0.06f else 0.12f) * ambientPulse),
                            accentColor.copy(alpha = 0.0f)
                        ),
                        center = Offset(w * 0.85f, h * 0.2f),
                        radius = w * 0.62f
                    ),
                    radius = w * 0.62f,
                    center = Offset(w * 0.85f, h * 0.2f)
                )
            }
            1, 2 -> { // Partly Cloudy (Sun ray + soft drifting cloud shape)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = (if (isNight) 0.05f else 0.09f) * ambientPulse),
                            accentColor.copy(alpha = 0.0f)
                        ),
                        center = Offset(w * 0.85f, h * 0.2f),
                        radius = w * 0.5f
                    ),
                    radius = w * 0.5f,
                    center = Offset(w * 0.85f, h * 0.2f)
                )
                
                val cloudArt = Path().apply {
                    val nx = w * 0.45f
                    val ny = h * 0.25f
                    moveTo(nx + w * 0.15f * 0.8f, ny + h * 0.25f * 0.8f)
                    lineTo(nx + w * 0.55f * 0.8f, ny + h * 0.25f * 0.8f)
                    cubicTo(nx + w * 0.65f * 0.8f, ny + h * 0.25f * 0.8f, nx + w * 0.65f * 0.8f, ny + h * 0.08f * 0.8f, nx + w * 0.55f * 0.8f, ny + h * 0.08f * 0.8f)
                    cubicTo(nx + w * 0.52f * 0.8f, ny - h * 0.08f * 0.8f, nx + w * 0.35f * 0.8f, ny - h * 0.08f * 0.8f, nx + w * 0.37f * 0.8f, ny + h * 0.04f * 0.8f)
                    cubicTo(nx + w * 0.27f * 0.8f, ny - h * 0.05f * 0.8f, nx + w * 0.12f * 0.8f, ny + h * 0.03f * 0.8f, nx + w * 0.17f * 0.8f, ny + h * 0.14f * 0.8f)
                    cubicTo(nx + w * 0.12f * 0.8f, ny + h * 0.19f * 0.8f, nx + w * 0.12f * 0.8f, ny + h * 0.25f * 0.8f, nx + w * 0.15f * 0.8f, ny + h * 0.25f * 0.8f)
                    close()
                }
                translate(left = cloudDrift) {
                    drawPath(
                        path = cloudArt,
                        color = (if (isNight) Color.White else Color.Black).copy(alpha = 0.04f)
                    )
                }
            }
            3, 45, 48 -> { // Cloudy / Fog (Overlapping giant soft clouds)
                val cloudArt1 = Path().apply {
                    val nx = -w * 0.1f
                    val ny = h * 0.35f
                    moveTo(nx + w * 0.15f * 1.5f, ny + h * 0.25f * 1.5f)
                    lineTo(nx + w * 0.55f * 1.5f, ny + h * 0.25f * 1.5f)
                    cubicTo(nx + w * 0.65f * 1.5f, ny + h * 0.25f * 1.5f, nx + w * 0.65f * 1.5f, ny + h * 0.08f * 1.5f, nx + w * 0.55f * 1.5f, ny + h * 0.08f * 1.5f)
                    cubicTo(nx + w * 0.52f * 1.5f, ny - h * 0.08f * 1.5f, nx + w * 0.35f * 1.5f, ny - h * 0.08f * 1.5f, nx + w * 0.37f * 1.5f, ny + h * 0.04f * 1.5f)
                    cubicTo(nx + w * 0.27f * 1.5f, ny - h * 0.05f * 1.5f, nx + w * 0.12f * 1.5f, ny + h * 0.03f * 1.5f, nx + w * 0.17f * 1.5f, ny + h * 0.14f * 1.5f)
                    cubicTo(nx + w * 0.12f * 1.5f, ny + h * 0.19f * 1.5f, nx + w * 0.12f * 1.5f, ny + h * 0.25f * 1.5f, nx + w * 0.15f * 1.5f, ny + h * 0.25f * 1.5f)
                    close()
                }
                translate(left = cloudDrift) {
                    drawPath(
                        path = cloudArt1,
                        color = (if (isNight) Color.White else Color.Black).copy(alpha = 0.03f)
                    )
                }
                
                val cloudArt2 = Path().apply {
                    val nx = w * 0.25f
                    val ny = h * 0.05f
                    moveTo(nx + w * 0.15f * 1.2f, ny + h * 0.25f * 1.2f)
                    lineTo(nx + w * 0.55f * 1.2f, ny + h * 0.25f * 1.2f)
                    cubicTo(nx + w * 0.65f * 1.2f, ny + h * 0.25f * 1.2f, nx + w * 0.65f * 1.2f, ny + h * 0.08f * 1.2f, nx + w * 0.55f * 1.2f, ny + h * 0.08f * 1.2f)
                    cubicTo(nx + w * 0.52f * 1.2f, ny - h * 0.08f * 1.2f, nx + w * 0.35f * 1.2f, ny - h * 0.08f * 1.2f, nx + w * 0.37f * 1.2f, ny + h * 0.04f * 1.2f)
                    cubicTo(nx + w * 0.27f * 1.2f, ny - h * 0.05f * 1.2f, nx + w * 0.12f * 1.2f, ny + h * 0.03f * 1.2f, nx + w * 0.17f * 1.2f, ny + h * 0.14f * 1.2f)
                    cubicTo(nx + w * 0.12f * 1.2f, ny + h * 0.19f * 1.2f, nx + w * 0.12f * 1.2f, ny + h * 0.25f * 1.2f, nx + w * 0.15f * 1.2f, ny + h * 0.25f * 1.2f)
                    close()
                }
                translate(left = -cloudDrift * 0.7f) {
                    drawPath(
                        path = cloudArt2,
                        color = (if (isNight) Color.White else Color.Black).copy(alpha = 0.05f)
                    )
                }
            }
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> { // Rain (Drifting drops)
                val rCloudArt = Path().apply {
                    val nx = w * 0.35f
                    val ny = h * 0.02f
                    moveTo(nx + w * 0.15f * 1.2f, ny + h * 0.25f * 1.2f)
                    lineTo(nx + w * 0.55f * 1.2f, ny + h * 0.25f * 1.2f)
                    cubicTo(nx + w * 0.65f * 1.2f, ny + h * 0.25f * 1.2f, nx + w * 0.65f * 1.2f, ny + h * 0.08f * 1.2f, nx + w * 0.55f * 1.2f, ny + h * 0.08f * 1.2f)
                    cubicTo(nx + w * 0.52f * 1.2f, ny - h * 0.08f * 1.2f, nx + w * 0.35f * 1.2f, ny - h * 0.08f * 1.2f, nx + w * 0.37f * 1.2f, ny + h * 0.04f * 1.2f)
                    cubicTo(nx + w * 0.27f * 1.2f, ny - h * 0.05f * 1.2f, nx + w * 0.12f * 1.2f, ny + h * 0.03f * 1.2f, nx + w * 0.17f * 1.2f, ny + h * 0.14f * 1.2f)
                    cubicTo(nx + w * 0.12f * 1.2f, ny + h * 0.19f * 1.2f, nx + w * 0.12f * 1.2f, ny + h * 0.25f * 1.2f, nx + w * 0.15f * 1.2f, ny + h * 0.25f * 1.2f)
                    close()
                }
                translate(left = cloudDrift * 0.5f) {
                    drawPath(
                        path = rCloudArt,
                        color = (if (isNight) Color.White else Color.Black).copy(alpha = 0.04f)
                    )
                }
                
                val rainColor = Color(0xFF38BDF8).copy(alpha = if (isNight) 0.11f else 0.24f)
                val spacingX = w / 6f
                val spacingY = h / 4f
                translate(left = -rainOffset * 0.2f, top = rainOffset) {
                    for (x in 1..5) {
                        for (y in 0..4) {
                            val startX = x * spacingX + (y * 8f)
                            val startY = y * spacingY - (x * 4f)
                            drawLine(
                                color = rainColor,
                                start = Offset(startX, startY),
                                end = Offset(startX - w * 0.05f, startY + h * 0.11f),
                                strokeWidth = w * 0.006f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
            71, 73, 75, 77, 85, 86 -> { // Snow (Crisp slow snowflakes dots)
                val snowColor = Color.White.copy(alpha = if (isNight) 0.18f else 0.35f)
                val driftPoints = listOf(
                    Offset(w * 0.15f, h * 0.25f), Offset(w * 0.35f, h * 0.15f), Offset(w * 0.75f, h * 0.20f),
                    Offset(w * 0.25f, h * 0.60f), Offset(w * 0.55f, h * 0.45f), Offset(w * 0.85f, h * 0.70f),
                    Offset(w * 0.45f, h * 0.80f), Offset(w * 0.68f, h * 0.75f), Offset(w * 0.10f, h * 0.85f)
                )
                translate(left = cloudDrift * 0.8f, top = rainOffset * 0.4f) {
                    driftPoints.forEach { pt ->
                        drawCircle(
                            color = snowColor,
                            radius = w * 0.015f,
                            center = pt
                        )
                    }
                }
            }
            95, 96, 99 -> { // Storm (Dark pulsing lightning aura)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFBBF24).copy(alpha = (if (isNight) 0.05f else 0.1f) * ambientPulse),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.5f
                    ),
                    radius = w * 0.5f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
        }
    }
}
