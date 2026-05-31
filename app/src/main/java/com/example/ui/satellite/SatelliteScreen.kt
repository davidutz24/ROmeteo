package com.example.ui.satellite

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.ui.webview.WebViewScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteScreen() {
    var selectedService by remember { mutableStateOf("sat24") }

    val urls = mapOf(
        "sat24" to "https://www.sat24.com/en-gb/country/ro/hd",
        "meteologix hd" to "https://meteologix.com/ro/satellite/satellite-hd",
        "ANM" to "https://www.meteoromania.ro/sateliti/index.php",
        "meteolab" to "https://meteolab.si/satelit.php"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = urls.keys.indexOf(selectedService),
            edgePadding = 8.dp
        ) {
            urls.keys.forEachIndexed { index, title ->
                Tab(
                    selected = selectedService == title,
                    onClick = { selectedService = title },
                    text = { Text(title) }
                )
            }
        }
        
        WebViewScreen(url = urls[selectedService]!!, modifier = Modifier.weight(1f))
    }
}
