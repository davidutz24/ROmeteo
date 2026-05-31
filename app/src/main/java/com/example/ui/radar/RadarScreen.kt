package com.example.ui.radar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.ui.webview.WebViewScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen() {
    var selectedService by remember { mutableStateOf("metmax") }

    val urls = mapOf(
        "metmax" to "https://metmax.app/radar/",
        "meteologix" to "https://meteologix.com/ro/radar-hd/romania",
        "blitzortung" to "https://map.blitzortung.org/",
        "lightning analysis" to "https://meteologix.com/ro/lightning/romania/"
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
