package com.rmm.recetasraquel.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    showDevelopmentTools: Boolean,
    onNavigateBack: () -> Unit,
    onLoadDemoData: () -> Unit,
    onRemoveDemoData: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Atrás" },
                    ) { Text("Volver") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Ajustes de la aplicación", style = MaterialTheme.typography.titleMedium)
            if (showDevelopmentTools) {
                Text("Desarrollo", style = MaterialTheme.typography.titleMedium)
                Text("Estas acciones solo están disponibles en la compilación debug.")
                Button(
                    onClick = onLoadDemoData,
                    enabled = !state.isWorking,
                    modifier = Modifier.testTag("load_demo_data"),
                ) { Text("Cargar recetas de demostración") }
                OutlinedButton(
                    onClick = onRemoveDemoData,
                    enabled = !state.isWorking,
                    modifier = Modifier.testTag("remove_demo_data"),
                ) { Text("Retirar recetas de demostración") }
            }
            state.message?.let { Text(it) }
        }
    }
}
