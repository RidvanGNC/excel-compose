<h1 align="center">excel-compose</h1>

<p align="center">
An Excel-style dense data grid for <a href="https://www.jetbrains.com/lp/compose-multiplatform/">Compose Multiplatform</a> (Android + Desktop) — resizable columns, click-to-sort headers, per-column filters, row selection, row tinting, and a synced horizontal scroll across header/filter/body. No <code>CompositionLocal</code> theming — colors are plain parameters, the same spirit as <code>Scaffold</code>'s <code>containerColor</code>.
</p>

<p align="center">
<a href="https://central.sonatype.com/artifact/io.github.ridvangnc/excel-compose"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/io.github.ridvangnc/excel-compose"></a>
<a href="https://github.com/RidvanGNC/excel-compose/releases/latest"><img alt="Latest Release" src="https://img.shields.io/github/v/release/RidvanGNC/excel-compose"></a>
<a href="https://github.com/RidvanGNC/excel-compose/actions/workflows/ci.yml"><img alt="build" src="https://github.com/RidvanGNC/excel-compose/actions/workflows/ci.yml/badge.svg"></a>
<img alt="kotlin" src="https://img.shields.io/badge/kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white">
<img alt="license" src="https://img.shields.io/github/license/RidvanGNC/excel-compose">
</p>

<p align="center">
<img alt="Kotlin Multiplatform" src="https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white">
<img alt="Android" src="https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white">
<img alt="Desktop" src="https://img.shields.io/badge/Desktop-4D5B9E?logo=jetpackcompose&logoColor=white">
</p>

<p align="center">
<a href="#english">English</a> · <a href="#türkçe">Türkçe</a>
</p>

---

## English

### Features

- **Resizable columns** — drag a header border like Excel; the rest of the grid doesn't jitter while you drag (only the column you're resizing changes size).
- **Click-to-sort headers** — the grid only renders the indicator (▴ ▾ ↕) and reports which column was clicked; you own the actual sorting logic.
- **Per-column filters** — a plain text box by default, or plug in `ChoiceFilterCell` / `MultiChoiceFilterCell` / your own composable.
- **Row selection** — an optional leading checkbox column with a tri-state "select all".
- **Row tinting & dimming** — color a row (e.g. by status) or fade it out (e.g. inactive records) with a simple per-row function.
- **No forced latency on taps** — single-tap and double-tap are each only as slow as they need to be; see [Tap behavior](#tap-behavior) below.
- **No CompositionLocal** — every color is an explicit parameter (`ExcelGridColors`), so there's nothing implicit to wire up in a host app's theme.
- **Storage-agnostic** — the grid keeps resized column widths in memory and hands you a callback to persist them however you like; it never assumes a specific storage layer.

### Install

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

### Quick start

```kotlin
data class Employee(val id: String, val name: String, val department: String, val salary: Int)

val columns = listOf(
    GridColumn<Employee>("name", "Name", 200.dp, sortable = true, value = { it.name }),
    GridColumn("department", "Department", 160.dp, value = { it.department }),
    GridColumn("salary", "Salary", 120.dp, sortable = true, value = { it.salary.toString() }),
)

DataGrid(
    columns = columns,
    rows = employees,
    key = { it.id },
    colors = ExcelGridDefaults.colors(containerColor = Color.White),
    selectable = true,
    selectedKeys = selectedKeys,
    onSelect = { key -> selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key },
    onSelectAll = { all -> selectedKeys = if (all) employees.map { it.id }.toSet() else emptySet() },
    sortId = sortId,
    sortDesc = sortDesc,
    onSort = { columnId -> /* flip sortId/sortDesc, re-sort `employees` yourself */ },
    onRowOpen = { row -> /* double-click: open/edit `row` */ },
)
```

See [`demo/`](demo) for a complete, runnable example (`./gradlew :demo:run`).

### Demo

#### Column resizing

Drag any header border to resize a column, like Excel — only the column you're dragging changes size, the rest hold steady.

<video src="video/column-resize.mp4" controls width="700"></video>

#### Sorting

Click a sortable header to sort ascending/descending; the indicator (▴ ▾ ↕) shows the current state.

<video src="video/sorting.mp4" controls width="700"></video>

#### Filtering

Type in a column's filter box, or use a dropdown (`ChoiceFilter`/`MultiChoiceFilter`) for a fixed set of values.

<video src="video/filtering.mp4" controls width="700"></video>

#### Row selection

An optional leading checkbox column with a tri-state "select all" in the header.

<video src="video/row-selection.mp4" controls width="700"></video>

### API reference

#### `DataGrid<T>`

The grid itself. All parameters have defaults except `columns`, `rows`, and `key`.

| Parameter | Type | What it does |
|---|---|---|
| `columns` | `List<GridColumn<T>>` | Column definitions, left to right. |
| `rows` | `List<T>` | The data to render, one row each. |
| `key` | `(T) -> String` | Stable identity for each row (used by `LazyColumn` and for selection). |
| `modifier` | `Modifier` | Applied to the whole grid. |
| `colors` | `ExcelGridColors` | See [below](#excelgridcolors--excelgriddefaults). Defaults to `ExcelGridDefaults.colors()`. |
| `selectable` | `Boolean` | Shows the leading checkbox column with a tri-state "select all" in the header. |
| `selectedKeys` | `Set<String>` | Which rows (by `key`) are currently selected. |
| `onSelect` | `(key: String) -> Unit` | A row's checkbox/single-tap was toggled. |
| `onSelectAll` | `(selectAll: Boolean) -> Unit` | The header checkbox was clicked. |
| `filters` | `Map<String, String>` | Current filter text per column id. |
| `onFilter` | `(columnId, value) -> Unit` | A filter box changed. |
| `loading` / `loadingNextPage` | `Boolean` | `loading` hides the empty-state text while true; `loadingNextPage` shows a spinner row at the bottom. |
| `onRowOpen` | `(T) -> Unit` | Double-click (or single-click if `openOnSingleTap`) on a row. |
| `onRowTap` | `((row: T, isDoubleTap: Boolean) -> Unit)?` | Escape hatch — fires on every tap *alongside* whatever `onSelect`/`onRowOpen` already do, so you can layer your own behavior without forking the grid. |
| `onNearEnd` | `() -> Unit` | Called once the list has scrolled within 8 rows of the end — wire up pagination here. |
| `initialColumnWidths` | `Map<String, Dp>` | Seeds column widths the user previously resized (e.g. loaded from your own storage). |
| `onColumnWidthChange` | `(columnId, width) -> Unit` | Fires once a drag finishes — persist it however you like. |
| `rowDimmed` | `((T) -> Boolean)?` | Row renders at 55% alpha when true (e.g. inactive records). |
| `rowTint` | `((T) -> Color?)?` | Non-null return tints that row's background (selection still wins visually). |
| `openOnSingleTap` | `Boolean` | Single tap opens the row instead of the default double tap. |
| `singleTapEnabled` / `doubleTapEnabled` / `doubleTapTimeoutMillis` | see [Tap behavior](#tap-behavior) | Fine control over single/double tap latency. |
| `sortId` / `sortDesc` | `String?` / `Boolean` | Which column is currently sorted, and in which direction — drawn as the header indicator. |
| `onSort` | `((columnId: String) -> Unit)?` | A sortable header was clicked; you decide what "sorted" means and re-supply `rows` accordingly. |
| `emptyState` | `@Composable () -> Unit` | Shown centered when `rows` is empty and `loading` is false. |

#### `GridColumn<T>`

```kotlin
data class GridColumn<T>(
    val id: String,
    val heading: String,
    val width: Dp,
    val cell: ExcelComposeCell<T>,
    val filter: ExcelComposeFilter = ExcelComposeFilter.TextFilter,
    val sortable: Boolean = false,
    val align: TextAlign = TextAlign.Start,
)

// Convenience constructor for the common case — plain text, no need to wrap it yourself:
fun <T> GridColumn(
    id: String, heading: String, width: Dp,
    filter: ExcelComposeFilter = ExcelComposeFilter.TextFilter,
    sortable: Boolean = false, align: TextAlign = TextAlign.Start,
    value: (T) -> String,
): GridColumn<T>
```

- `width` is a **base** width — columns grow proportionally to fill any extra space when the grid is wider than the sum of all column widths; otherwise the grid scrolls horizontally.
- `cell` picks what the cell body renders — see [`ExcelComposeCell`](#excelcomposecell) below. Most columns just pass `value = { ... }` and never touch `cell` directly; that goes through the convenience constructor.
- `filter` picks what the filter-row box looks like — see [`ExcelComposeFilter`](#excelcomposefilter) below.

#### `ExcelComposeCell`

A typed alternative to a loose `value: (T) -> String` + nullable `content: @Composable (T) -> Unit` pair, same spirit as `ExcelComposeFilter`:

```kotlin
sealed interface ExcelComposeCell<T> {
    data class TextCell<T>(val value: (T) -> String) : ExcelComposeCell<T>
    data class CustomCell<T>(val content: @Composable (T) -> Unit) : ExcelComposeCell<T>
}
```

```kotlin
GridColumn(
    id = "salary", heading = "Salary", width = 120.dp,
    cell = ExcelComposeCell.CustomCell { employee ->
        Row {
            Text(employee.salary.toString())
            if (employee.salary >= 95000) Text(" ★", color = Color(0xFFB8860B))
        }
    },
)
```

#### `ExcelGridColors` / `ExcelGridDefaults`

```kotlin
data class ExcelGridColors(
    val containerColor: Color,
    val headerContainerColor: Color,
    val filterContainerColor: Color,
    val rowContainerColor: Color,
    val selectedRowContainerColor: Color,
    val lineColor: Color,
)
```

No `CompositionLocal` — just pass an `ExcelGridColors` in. `ExcelGridDefaults.colors(...)` gives you sensible Material 3-derived defaults for any subset you don't override, e.g.:

```kotlin
ExcelGridDefaults.colors(containerColor = MyBrand.background, selectedRowContainerColor = MyBrand.accent)
```

#### `ExcelComposeFilter`

A typed, discoverable choice for a column's `filter` — instead of a loose `filterable: Boolean` + nullable `@Composable` lambda pair, pick a named case:

```kotlin
sealed interface ExcelComposeFilter {
    data object NoFilter : ExcelComposeFilter
    data object TextFilter : ExcelComposeFilter
    data class ChoiceFilter(val options: List<Pair<String, String>>) : ExcelComposeFilter
    data class MultiChoiceFilter(val options: List<Pair<String, String>>, val allLabel: String) : ExcelComposeFilter
    data class CustomFilter(val content: @Composable (current: String, onChange: (String) -> Unit) -> Unit) : ExcelComposeFilter
}
```

| Case | What you get |
|---|---|
| `NoFilter` | No box at all for this column. |
| `TextFilter` | A plain text box — the default when `filter` isn't specified. |
| `ChoiceFilter(options)` | Single-choice dropdown; `options` are `(value, label)` pairs, the first meaning "all". |
| `MultiChoiceFilter(options, allLabel)` | Multi-choice dropdown; selection is carried as a comma-separated string in the filter map. |
| `CustomFilter(content)` | Escape hatch — any composable you write yourself, for anything the built-in cases don't cover. |

```kotlin
GridColumn(
    "department", "Department", 160.dp, value = { it.department },
    filter = ExcelComposeFilter.ChoiceFilter(
        listOf("" to "All", "Engineering" to "Engineering", "Research" to "Research"),
    ),
)
```

`ChoiceFilterCell`/`MultiChoiceFilterCell` (the composables backing `ChoiceFilter`/`MultiChoiceFilter`) are also exported directly, in case you want to reuse them outside of a `GridColumn`.

#### Tap behavior

There is no way to tell a single click from the first half of a double click without *either* waiting to see if a second tap follows, *or* accepting that a double click also fires the single-tap action once before the double-tap action. `DataGrid` only pays that cost when it actually has to:

| `singleTapEnabled` | `doubleTapEnabled` | What happens |
|---|---|---|
| ✅ | ❌ | `onTap`-side behavior fires **instantly** — nothing to disambiguate against. |
| ❌ | ✅ | Waits up to `doubleTapTimeoutMillis` for a second tap; a lone tap does nothing, so the wait costs nothing either. |
| ✅ | ✅ | Every tap waits up to `doubleTapTimeoutMillis`: fires the single-tap behavior if no second tap arrives, or the double-tap behavior instead (not in addition) if one does. This is the one combination with real, unavoidable latency — because it's the one case where a tap is genuinely ambiguous. |

`singleTapEnabled` defaults to `selectable || openOnSingleTap` (i.e., true only when the grid actually does something on a single tap); override it explicitly if you rely purely on `onRowTap` for single-tap behavior. `doubleTapTimeoutMillis` defaults to `300L`.

#### Column resizing & persistence

The grid keeps resized widths in memory for its own lifetime. To persist them across app restarts, load your own saved widths into `initialColumnWidths` and save whatever `onColumnWidthChange` reports — the library has no opinion on *how* you store it (a file, a database, `SharedPreferences`, anything).

### License

MIT — see [LICENSE](LICENSE).

---

## Türkçe

### Özellikler

- **Kolon genişliği ayarlanabilir** — Excel'deki gibi başlık sınırından sürükle; sürüklerken diğer kolonlar titremiyor (sadece sürüklediğin kolon boyut değiştiriyor).
- **Başlığa tıklayarak sıralama** — grid yalnızca ok göstergesini (▴ ▾ ↕) çiziyor ve hangi kolona tıklandığını bildiriyor; asıl sıralama mantığı sende.
- **Kolon bazlı filtreler** — varsayılan olarak düz metin kutusu, istersen `ChoiceFilterCell` / `MultiChoiceFilterCell` ya da kendi composable'ını bağlarsın.
- **Satır seçimi** — opsiyonel, sol başta üç durumlu ("hepsini seç") checkbox kolonu.
- **Satır renklendirme ve soluklaştırma** — bir satırı (örn. duruma göre) renklendir ya da (örn. pasif kayıtlar için) soluklaştır, basit bir satır-bazlı fonksiyonla.
- **Dokunmada zorunlu gecikme yok** — tek ve çift tıklama, sadece gerçekten gerektiği kadar yavaş; aşağıdaki [Dokunma davranışı](#dokunma-davranışı) bölümüne bak.
- **CompositionLocal yok** — her renk açık bir parametre (`ExcelGridColors`), uygulamanın temasına örtük bir şekilde bağlanan hiçbir şey yok.
- **Depolamadan bağımsız** — grid, sürükleyerek değiştirilen kolon genişliklerini bellekte tutar ve sana bir callback verir; nasıl saklayacağın (dosya, veritabanı, her neyse) tamamen sana kalmış.

### Kurulum

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
    implementation("io.github.ridvangnc:excel-compose:<sürüm>")
}
```

### Hızlı başlangıç

```kotlin
data class Employee(val id: String, val name: String, val department: String, val salary: Int)

val columns = listOf(
    GridColumn<Employee>("name", "Name", 200.dp, sortable = true, value = { it.name }),
    GridColumn("department", "Department", 160.dp, value = { it.department }),
    GridColumn("salary", "Salary", 120.dp, sortable = true, value = { it.salary.toString() }),
)

DataGrid(
    columns = columns,
    rows = employees,
    key = { it.id },
    colors = ExcelGridDefaults.colors(containerColor = Color.White),
    selectable = true,
    selectedKeys = selectedKeys,
    onSelect = { key -> selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key },
    onSelectAll = { all -> selectedKeys = if (all) employees.map { it.id }.toSet() else emptySet() },
    sortId = sortId,
    sortDesc = sortDesc,
    onSort = { columnId -> /* sortId/sortDesc'i çevir, `employees`'i kendin yeniden sırala */ },
    onRowOpen = { row -> /* çift tıklama: `row`'u aç/düzenle */ },
)
```

Tam, çalıştırılabilir bir örnek için [`demo/`](demo)'ya bak (`./gradlew :demo:run`).

### Demo

#### Kolon boyutlandırma

Excel'deki gibi herhangi bir başlık sınırını sürükleyerek kolonu boyutlandır — sadece sürüklediğin kolon değişir, diğerleri yerinde kalır.

<video src="video/column-resize.mp4" controls width="700"></video>

#### Sıralama

Sıralanabilir bir başlığa tıkla, artan/azalan sıralanır; gösterge (▴ ▾ ↕) o anki durumu gösterir.

<video src="video/sorting.mp4" controls width="700"></video>

#### Filtreleme

Bir kolonun filtre kutusuna yaz, ya da sabit bir değer kümesi için açılır liste (`ChoiceFilter`/`MultiChoiceFilter`) kullan.

<video src="video/filtering.mp4" controls width="700"></video>

#### Satır seçimi

Başlıkta üç durumlu "hepsini seç" içeren opsiyonel, sol başta bir checkbox kolonu.

<video src="video/row-selection.mp4" controls width="700"></video>

### API referansı

#### `DataGrid<T>`

Grid'in kendisi. `columns`, `rows`, `key` dışındaki tüm parametrelerin varsayılan değeri var.

| Parametre | Tip | Ne işe yarar |
|---|---|---|
| `columns` | `List<GridColumn<T>>` | Soldan sağa kolon tanımları. |
| `rows` | `List<T>` | Çizilecek veri, her biri bir satır. |
| `key` | `(T) -> String` | Her satır için sabit kimlik (`LazyColumn` ve seçim için kullanılır). |
| `modifier` | `Modifier` | Tüm grid'e uygulanır. |
| `colors` | `ExcelGridColors` | [Aşağıya](#excelgridcolors--excelgriddefaults) bak. Varsayılan: `ExcelGridDefaults.colors()`. |
| `selectable` | `Boolean` | Başlıkta üç durumlu "hepsini seç" checkbox'ı olan sol kolonu gösterir. |
| `selectedKeys` | `Set<String>` | Hangi satırların (`key`'e göre) şu an seçili olduğu. |
| `onSelect` | `(key: String) -> Unit` | Bir satırın checkbox'ı/tek dokunuşu değişti. |
| `onSelectAll` | `(selectAll: Boolean) -> Unit` | Başlıktaki checkbox'a tıklandı. |
| `filters` | `Map<String, String>` | Her kolon için o anki filtre metni. |
| `onFilter` | `(columnId, value) -> Unit` | Bir filtre kutusu değişti. |
| `loading` / `loadingNextPage` | `Boolean` | `loading` true iken boş-durum metnini gizler; `loadingNextPage` altta bir spinner satırı gösterir. |
| `onRowOpen` | `(T) -> Unit` | Bir satıra çift tıklama (ya da `openOnSingleTap` açıksa tek tıklama). |
| `onRowTap` | `((row: T, isDoubleTap: Boolean) -> Unit)?` | Kaçış kapısı — `onSelect`/`onRowOpen`'ın yaptığının **yanında**, her dokunuşta tetiklenir; grid'i çatallamadan kendi davranışını ekleyebilirsin. |
| `onNearEnd` | `() -> Unit` | Liste sona 8 satır kalana kadar kaydırıldığında bir kez çağrılır — sayfalama burada bağlanır. |
| `initialColumnWidths` | `Map<String, Dp>` | Kullanıcının daha önce sürükleyerek ayarladığı kolon genişliklerini besler (ör. kendi deponuzdan yüklenmiş). |
| `onColumnWidthChange` | `(columnId, width) -> Unit` | Bir sürükleme bitince bir kez tetiklenir — istediğin gibi kalıcı hale getir. |
| `rowDimmed` | `((T) -> Boolean)?` | true dönerse satır %55 alfa ile çizilir (ör. pasif kayıtlar). |
| `rowTint` | `((T) -> Color?)?` | null olmayan dönüş o satırın zeminini boyar (seçim yine de görsel olarak öne geçer). |
| `openOnSingleTap` | `Boolean` | Varsayılan çift tıklama yerine tek tıklama satırı açar. |
| `singleTapEnabled` / `doubleTapEnabled` / `doubleTapTimeoutMillis` | [Dokunma davranışı](#dokunma-davranışı)'na bak | Tek/çift dokunuş gecikmesi üzerinde ince kontrol. |
| `sortId` / `sortDesc` | `String?` / `Boolean` | Şu an hangi kolonun, hangi yönde sıralı olduğu — başlık göstergesi olarak çizilir. |
| `onSort` | `((columnId: String) -> Unit)?` | Sıralanabilir bir başlığa tıklandı; "sıralı" ne demek sen karar verirsin, `rows`'u ona göre yeniden verirsin. |
| `emptyState` | `@Composable () -> Unit` | `rows` boşken ve `loading` false iken ortada gösterilir. |

#### `GridColumn<T>`

```kotlin
data class GridColumn<T>(
    val id: String,
    val heading: String,
    val width: Dp,
    val cell: ExcelComposeCell<T>,
    val filter: ExcelComposeFilter = ExcelComposeFilter.TextFilter,
    val sortable: Boolean = false,
    val align: TextAlign = TextAlign.Start,
)

// Yaygın durum için kolaylık constructor'ı — düz metin, kendin sarmalamana gerek yok:
fun <T> GridColumn(
    id: String, heading: String, width: Dp,
    filter: ExcelComposeFilter = ExcelComposeFilter.TextFilter,
    sortable: Boolean = false, align: TextAlign = TextAlign.Start,
    value: (T) -> String,
): GridColumn<T>
```

- `width` bir **taban** genişliktir — grid, tüm kolon genişlikleri toplamından daha genişse kolonlar orantılı olarak büyüyerek boşluğu doldurur; aksi halde grid yatay kayar.
- `cell`, hücre gövdesinin neyi çizeceğini seçer — aşağıdaki [`ExcelComposeCell`](#excelcomposecell-1)'e bak. Çoğu kolon sadece `value = { ... }` verir, `cell`'e hiç dokunmaz — bu, kolaylık constructor'ından geçer.
- `filter`, filtre satırındaki kutunun neye benzeyeceğini seçer — aşağıdaki [`ExcelComposeFilter`](#excelcomposefilter-1)'e bak.

#### `ExcelComposeCell`

Gevşek bir `value: (T) -> String` + nullable `content: @Composable (T) -> Unit` ikilisine tipli bir alternatif, `ExcelComposeFilter` ile aynı ruhta:

```kotlin
sealed interface ExcelComposeCell<T> {
    data class TextCell<T>(val value: (T) -> String) : ExcelComposeCell<T>
    data class CustomCell<T>(val content: @Composable (T) -> Unit) : ExcelComposeCell<T>
}
```

```kotlin
GridColumn(
    id = "maas", heading = "Maaş", width = 120.dp,
    cell = ExcelComposeCell.CustomCell { calisan ->
        Row {
            Text(calisan.maas.toString())
            if (calisan.maas >= 95000) Text(" ★", color = Color(0xFFB8860B))
        }
    },
)
```

#### `ExcelGridColors` / `ExcelGridDefaults`

```kotlin
data class ExcelGridColors(
    val containerColor: Color,
    val headerContainerColor: Color,
    val filterContainerColor: Color,
    val rowContainerColor: Color,
    val selectedRowContainerColor: Color,
    val lineColor: Color,
)
```

`CompositionLocal` yok — sadece bir `ExcelGridColors` geç. `ExcelGridDefaults.colors(...)`, override etmediğin her alan için Material 3 tabanlı makul varsayılanlar verir, örn.:

```kotlin
ExcelGridDefaults.colors(containerColor = MarkamRengim.zemin, selectedRowContainerColor = MarkamRengim.vurgu)
```

#### `ExcelComposeFilter`

Bir kolonun `filter`'ı için tip-güvenli, keşfedilebilir bir seçim — gevşek bir `filterable: Boolean` + nullable `@Composable` lambda ikilisi yerine, isimli bir case seçiyorsun:

```kotlin
sealed interface ExcelComposeFilter {
    data object NoFilter : ExcelComposeFilter
    data object TextFilter : ExcelComposeFilter
    data class ChoiceFilter(val options: List<Pair<String, String>>) : ExcelComposeFilter
    data class MultiChoiceFilter(val options: List<Pair<String, String>>, val allLabel: String) : ExcelComposeFilter
    data class CustomFilter(val content: @Composable (current: String, onChange: (String) -> Unit) -> Unit) : ExcelComposeFilter
}
```

| Case | Ne verir |
|---|---|
| `NoFilter` | Bu kolon için hiç kutu yok. |
| `TextFilter` | Düz metin kutusu — `filter` belirtilmezse varsayılan. |
| `ChoiceFilter(options)` | Tek seçimli açılır liste; `options` `(değer, etiket)` çiftleri, ilki "hepsi" anlamına gelir. |
| `MultiChoiceFilter(options, allLabel)` | Çoklu seçim açılır liste; seçim filtre haritasında virgülle ayrılmış bir metin olarak taşınır. |
| `CustomFilter(content)` | Kaçış kapısı — hazır case'lerin karşılamadığı her şey için kendi yazdığın composable. |

```kotlin
GridColumn(
    "bolge", "Bölge", 160.dp, value = { it.bolge },
    filter = ExcelComposeFilter.ChoiceFilter(
        listOf("" to "Hepsi", "Bornova" to "Bornova", "Kemalpaşa" to "Kemalpaşa"),
    ),
)
```

`ChoiceFilterCell`/`MultiChoiceFilterCell` (`ChoiceFilter`/`MultiChoiceFilter`'ın arkasındaki composable'lar) doğrudan da dışa açık — bir `GridColumn` dışında tekrar kullanmak istersen.

#### Dokunma davranışı

Bir ikinci dokunuşun gelip gelmeyeceğini beklemeden, ya da çift tıklamanın tek-tıklama etkisini bir kez tetikleyip tetiklemediğini kabul etmeden, tek tıklamayı çift tıklamanın ilk yarısından ayırmanın bir yolu yok. `DataGrid` bu bedeli yalnızca gerçekten gerektiğinde ödüyor:

| `singleTapEnabled` | `doubleTapEnabled` | Ne oluyor |
|---|---|---|
| ✅ | ❌ | Tek dokunuş davranışı **anında** tetiklenir — ayırt edilecek bir şey yok. |
| ❌ | ✅ | İkinci dokunuş için `doubleTapTimeoutMillis` kadar beklenir; tek bir dokunuş hiçbir şey yapmaz, o yüzden bekleme kimseye maliyet çıkarmaz. |
| ✅ | ✅ | Her dokunuş `doubleTapTimeoutMillis` kadar bekler: ikinci dokunuş gelmezse tek-tıklama davranışı, gelirse (onun YERİNE, ek olarak değil) çift-tıklama davranışı tetiklenir. Gerçek, kaçınılmaz gecikmenin olduğu tek kombinasyon bu — çünkü bir dokunuşun gerçekten belirsiz olduğu tek durum bu. |

`singleTapEnabled` varsayılan olarak `selectable || openOnSingleTap` (yani grid tek dokunuşta gerçekten bir şey yapıyorsa true); tek-tıklama davranışını sadece `onRowTap` üzerinden yürütüyorsan elle `true` yap. `doubleTapTimeoutMillis` varsayılanı `300L`.

#### Kolon genişliği ve kalıcılık

Grid, sürükleyerek ayarlanan genişlikleri kendi yaşam süresi boyunca bellekte tutar. Uygulama yeniden başlatıldığında da kalıcı olsun istiyorsan, kendi kaydettiğin genişlikleri `initialColumnWidths`'e yükle, `onColumnWidthChange`'in bildirdiğini de istediğin yere kaydet — kütüphanenin *nasıl* sakladığın konusunda hiçbir görüşü yok (dosya, veritabanı, `SharedPreferences`, ne olursa).

### Lisans

MIT — bkz. [LICENSE](LICENSE).
