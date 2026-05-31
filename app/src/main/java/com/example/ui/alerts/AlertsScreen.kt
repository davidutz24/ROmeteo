package com.example.ui.alerts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.ui.webview.WebViewScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen() {
    var selectedService by remember { mutableStateOf("ANM Nowcasting") }

    val urls = mapOf(
        "ANM Nowcasting" to "https://www.meteoromania.ro/avertizari-nowcasting/",
        "ANM Avertizări" to "https://www.meteoromania.ro/avertizari/",
        "StormForecast" to "https://stormforecast.eu/",
        "Estofex" to "https://www.estofex.org/"
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
