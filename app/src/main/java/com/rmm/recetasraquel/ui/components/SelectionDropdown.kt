package com.rmm.recetasraquel.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionDropdown(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Seleccionar",
    clearLabel: String? = null,
    testTag: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        val fieldModifier = Modifier
            .menuAnchor(
                type = MenuAnchorType.PrimaryNotEditable,
                enabled = true,
            )
            .fillMaxWidth()
            .let { base -> if (testTag == null) base else base.testTag(testTag) }

        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = fieldModifier,
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (clearLabel != null && value.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text(clearLabel) },
                    onClick = {
                        onSelect("")
                        expanded = false
                    },
                )
            }
            options.distinct().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
