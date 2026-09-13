package excelcompose.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Single-choice filter dropdown for the filter row. [options] are (value, label) pairs;
 * the first option means "all" and carries an empty-string value.
 */
@Composable
fun ChoiceFilterCell(
    current: String,
    options: List<Pair<String, String>>,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    onChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == current }?.second
        ?: options.firstOrNull()?.second.orEmpty()
    Box {
        Row(
            Modifier.fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .clickable { open = true }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = if (current.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { open = false; onChange(value) })
            }
        }
    }
}

/**
 * Multi-choice filter dropdown. Selection is carried as a comma-separated value string
 * in the filter map; empty means "all".
 */
@Composable
fun MultiChoiceFilterCell(
    current: String,
    options: List<Pair<String, String>>,
    allLabel: String,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    onChange: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val selected = current.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val label = if (selected.isEmpty()) allLabel
    else options.filter { it.first in selected }.joinToString(", ") { it.second }
    Box {
        Row(
            Modifier.fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .clickable { open = true }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label, Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = if (selected.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = value in selected, onCheckedChange = null)
                            Text(text, Modifier.padding(start = 4.dp))
                        }
                    },
                    onClick = {
                        val next = if (value in selected) selected - value else selected + value
                        onChange(next.joinToString(","))
                    },
                )
            }
        }
    }
}
