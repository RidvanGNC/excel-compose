package excelcompose.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import excelcompose.grid.DataGrid
import excelcompose.grid.ExcelComposeCell
import excelcompose.grid.ExcelComposeFilter
import excelcompose.grid.ExcelGridDefaults
import excelcompose.grid.GridColumn

private data class Employee(val id: String, val name: String, val department: String, val salary: Int, val active: Boolean)

private val SAMPLE = listOf(
    Employee("1", "Ada Lovelace", "Engineering", 92000, true),
    Employee("2", "Grace Hopper", "Engineering", 98000, true),
    Employee("3", "Alan Turing", "Research", 89000, true),
    Employee("4", "Margaret Hamilton", "Engineering", 95000, false),
    Employee("5", "Katherine Johnson", "Research", 91000, true),
    Employee("6", "Hedy Lamarr", "R&D", 87000, true),
    Employee("7", "Radia Perlman", "Engineering", 99000, true),
    Employee("8", "Barbara Liskov", "Engineering", 97000, false),
)

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "excel-compose demo") {
        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                DemoScreen()
            }
        }
    }
}

@Composable
private fun DemoScreen() {
    var filters by remember { mutableStateOf(mapOf<String, String>()) }
    var sortId by remember { mutableStateOf<String?>(null) }
    var sortDesc by remember { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    var lastTap by remember { mutableStateOf("no tap yet") }
    var editable by remember { mutableStateOf(true) }
    var addColumnDialogOpen by remember { mutableStateOf(false) }
    var newColumnText by remember { mutableStateOf("") }
    var customColumnCounter by remember { mutableStateOf(0) }

    // Mutable (not the original `remember { listOf(...) }`) so onAddColumn/onDeleteColumn/
    // onReorder below have something to actually mutate — same pattern a real host app
    // would use, since the grid itself never touches this list on its own.
    var columns by remember {
        mutableStateOf(
            listOf(
                GridColumn<Employee>("name", "Name", 200.dp, sortable = true, value = { it.name }),
                GridColumn(
                    "department", "Department", 160.dp, value = { it.department },
                    filter = ExcelComposeFilter.ChoiceFilter(
                        listOf("" to "All", "Engineering" to "Engineering", "Research" to "Research", "R&D" to "R&D"),
                    ),
                ),
                GridColumn(
                    id = "salary", heading = "Salary", width = 120.dp, sortable = true,
                    // CustomCell example: a star next to well-paid rows, still plain text
                    // otherwise. copyValue is what makes "Copy cell"/"Copy row" work for this
                    // column too — without it, a CustomCell has no text to offer either item.
                    cell = ExcelComposeCell.CustomCell<Employee>(
                        content = { emp ->
                            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                Text(emp.salary.toString(), style = MaterialTheme.typography.bodySmall)
                                if (emp.salary >= 95000) {
                                    Text(" ★", color = Color(0xFFB8860B), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        copyValue = { it.salary.toString() },
                    ),
                ),
            ),
        )
    }

    var rows = SAMPLE.filter { row ->
        filters.all { (colId, v) ->
            v.isBlank() || when (colId) {
                "name" -> row.name.contains(v, ignoreCase = true)
                "department" -> row.department.contains(v, ignoreCase = true)
                "salary" -> row.salary.toString().contains(v)
                else -> true
            }
        }
    }
    if (sortId != null) {
        rows = when (sortId) {
            "name" -> rows.sortedBy { it.name }
            "salary" -> rows.sortedBy { it.salary }
            else -> rows
        }
        if (sortDesc) rows = rows.reversed()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Last tap: $lastTap", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Editable columns", style = MaterialTheme.typography.labelMedium)
            Switch(checked = editable, onCheckedChange = { editable = it }, modifier = Modifier.padding(start = 8.dp))
        }
        DataGrid(
            columns = columns,
            rows = rows,
            key = { it.id },
            modifier = Modifier.fillMaxSize().weight(1f),
            colors = ExcelGridDefaults.colors(),
            selectable = true,
            selectedKeys = selectedKeys,
            onSelect = { k -> selectedKeys = if (k in selectedKeys) selectedKeys - k else selectedKeys + k },
            onSelectAll = { all -> selectedKeys = if (all) rows.map { it.id }.toSet() else emptySet() },
            filters = filters,
            onFilter = { colId, v -> filters = filters + (colId to v) },
            rowDimmed = { !it.active },
            rowTint = { if (it.salary >= 95000) Color(0xFFDFF5E1) else null },
            sortId = sortId,
            sortDesc = sortDesc,
            onSort = { colId ->
                if (sortId == colId) sortDesc = !sortDesc else { sortId = colId; sortDesc = false }
            },
            onRowOpen = { },
            copyCellLabel = "Copy cell",
            copyRowLabel = "Copy row",
            copySelectedRowsLabel = "Copy selected rows",
            // Escape hatch demo: host app reacts to raw taps without forking the grid.
            onRowTap = { row, isDoubleTap ->
                lastTap = "${row.name} (${if (isDoubleTap) "double" else "single"})"
            },
            editable = editable,
            onAddColumn = { newColumnText = ""; addColumnDialogOpen = true },
            onDeleteColumn = { colId -> columns = columns.filterNot { it.id == colId } },
            onReorder = { from, to ->
                columns = columns.toMutableList().apply { add(to, removeAt(from)) }
            },
        )
    }

    if (addColumnDialogOpen) {
        AlertDialog(
            onDismissRequest = { addColumnDialogOpen = false },
            title = { Text("New column") },
            text = {
                // Whatever is typed here becomes both the column's heading AND its cell
                // content (repeated per row) — there's no real Employee field to bind a
                // freeform column to, so this just proves the onAddColumn round-trip end
                // to end rather than pretending to be a realistic column.
                OutlinedTextField(
                    value = newColumnText,
                    onValueChange = { newColumnText = it },
                    label = { Text("Column name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newColumnText.isNotBlank(),
                    onClick = {
                        val label = newColumnText.trim()
                        val id = "custom-${customColumnCounter++}"
                        columns = columns + GridColumn<Employee>(id, label, 140.dp, value = { label })
                        addColumnDialogOpen = false
                    },
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { addColumnDialogOpen = false }) { Text("Cancel") } },
        )
    }
}
