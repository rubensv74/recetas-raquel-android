package com.rmm.recetasraquel.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rmm.recetasraquel.domain.repository.DemoDataController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(val isWorking: Boolean = false, val message: String? = null)

class SettingsViewModel(
    private val demoDataController: DemoDataController?,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    val hasDevelopmentTools: Boolean = demoDataController != null

    fun loadDemoData() = runDemoAction("Recetas de demostración cargadas.") { load() }

    fun removeDemoData() = runDemoAction("Recetas de demostración retiradas.") { remove() }

    private fun runDemoAction(successMessage: String, operation: suspend DemoDataController.() -> Result<Unit>) {
        val controller = demoDataController ?: return
        viewModelScope.launch {
            mutableState.value = SettingsUiState(isWorking = true)
            val result = controller.operation()
            mutableState.value = SettingsUiState(
                message = if (result.isSuccess) successMessage else "No se pudo completar la operación.",
            )
        }
    }

    companion object {
        fun factory(controller: DemoDataController?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(controller) as T
            }
    }
}
