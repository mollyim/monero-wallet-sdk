package im.molly.monero.demo.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T>SelectListBox(
    label: String,
    options: Map<T, String>,
    selectedOption: T,
    onOptionClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        },
        modifier = modifier,
    ) {
        OutlinedTextField(
            readOnly = true,
            value = options.getValue(selectedOption),
            onValueChange = { },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label) },
            trailingIcon = {
                if (enabled) {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                }
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier.exposedDropdownSize(),
        ) {
            options.forEach { (key, text) ->
                DropdownMenuItem(
                    text = {
                        Text(text)
                    },
                    onClick = {
                        onOptionClick(key)
                        expanded = false
                    },
                )
            }
        }
    }
}
