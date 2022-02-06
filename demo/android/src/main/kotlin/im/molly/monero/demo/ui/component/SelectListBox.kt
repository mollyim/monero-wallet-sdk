package im.molly.monero.demo.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectListBox(
    @StringRes labelRes: Int,
    options: List<String>,
    selectedOption: String,
    onOptionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
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
            value = selectedOption,
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(stringResource(labelRes)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
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
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = {
                        Text(selectionOption)
                    },
                    onClick = {
                        onOptionClick(selectionOption)
                        expanded = false
                    },
                )
            }
        }
    }
}
