package excelcompose.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import excelcompose.grid.DataGrid
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

    val columns = remember {
        listOf(
            GridColumn<Employee>("name", "Name", 200.dp, sortable = true, value = { it.name }),
            GridColumn(
                "department", "Department", 160.dp, value = { it.department },
                filter = ExcelComposeFilter.ChoiceFilter(
                    listOf("" to "All", "Engineering" to "Engineering", "Research" to "Research", "R&D" to "R&D"),
                ),
            ),
            GridColumn("salary", "Salary", 120.dp, sortable = true, value = { it.salary.toString() }),
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
            // Escape hatch demo: host app reacts to raw taps without forking the grid.
            onRowTap = { row, isDoubleTap ->
                lastTap = "${row.name} (${if (isDoubleTap) "double" else "single"})"
            },
        )
    }
}
