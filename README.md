# excel-compose

An Excel-style dense data grid for Compose Multiplatform (Android + Desktop): resizable
columns, click-to-sort headers, per-column filters, row selection, row tinting, and a
synced horizontal scroll across header/filter/body — all sharing one theming surface with
no CompositionLocal, in the same spirit as `Scaffold`'s `containerColor`.

## Install

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts (commonMain)
dependencies {
    implementation("io.github.ridvangnc:excel-compose:<version>")
}
```

## Usage

```kotlin
DataGrid(
    columns = listOf(
        GridColumn("name", "Name", 200.dp, sortable = true, value = { it.name }),
        GridColumn("department", "Department", 160.dp, value = { it.department }),
    ),
    rows = employees,
    key = { it.id },
    colors = ExcelGridDefaults.colors(containerColor = Color.White),
    selectable = true,
    selectedKeys = selectedKeys,
    onSelect = { key -> /* toggle selection */ },
    onSelectAll = { all -> /* select/clear all */ },
    onRowOpen = { row -> /* double-click to open */ },
)
```

See [`demo/`](demo) for a complete, runnable example (`./gradlew :demo:run`).

## Module layout

- [`grid/`](grid) — the library itself (`DataGrid`, `GridColumn`, `ExcelGridColors`, filter
  cells). No dependency on any specific app's theme or storage — column widths and colors
  are plain parameters, and persisting a resized column is left to the host app via a
  callback.
- [`demo/`](demo) — a small desktop app exercising sorting, filtering, resizing, selection,
  and the `onRowTap` escape hatch.

## License

MIT — see [LICENSE](LICENSE).
