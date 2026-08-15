package com.gelengeden.app.data

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Minimal Office Open XML (.xlsx) writer. Produces a real workbook that Excel,
 * Google Sheets, and LibreOffice open without format/extension warnings.
 */
internal class XlsxWorkbook(
    private val rightToLeft: Boolean = false
) {
    private val sheets = mutableListOf<Sheet>()

    fun addSheet(
        name: String,
        columns: List<Column> = emptyList(),
        autoFilter: Boolean = false
    ): Sheet {
        val sheet = Sheet(
            name = uniqueSheetName(name),
            columns = columns,
            autoFilter = autoFilter,
            rightToLeft = rightToLeft
        )
        sheets += sheet
        return sheet
    }

    fun toByteArray(): ByteArray {
        require(sheets.isNotEmpty()) { "Workbook must have at least one sheet" }
        val shared = SharedStrings()
        val sheetXml = sheets.map { it.toXml(shared) }

        val out = ByteArrayOutputStream()
        ZipOutputStream(out, Charsets.UTF_8).use { zip ->
            zip.putText("[Content_Types].xml", contentTypesXml())
            zip.putText("_rels/.rels", rootRelsXml())
            zip.putText("xl/workbook.xml", workbookXml())
            zip.putText("xl/_rels/workbook.xml.rels", workbookRelsXml())
            zip.putText("xl/styles.xml", STYLES_XML)
            zip.putText("xl/sharedStrings.xml", shared.toXml())
            sheetXml.forEachIndexed { index, xml ->
                zip.putText("xl/worksheets/sheet${index + 1}.xml", xml)
            }
        }
        return out.toByteArray()
    }

    private fun uniqueSheetName(raw: String): String {
        val base = sanitizeSheetName(raw)
        if (sheets.none { it.name.equals(base, ignoreCase = true) }) return base
        var n = 2
        while (true) {
            val suffix = " $n"
            val candidate = (base.take(31 - suffix.length) + suffix).trim()
            if (sheets.none { it.name.equals(candidate, ignoreCase = true) }) return candidate
            n++
        }
    }

    private fun contentTypesXml(): String {
        val sheetOverrides = sheets.indices.joinToString("\n  ") { i ->
            """<Override PartName="/xl/worksheets/sheet${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
        }
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
              <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
              $sheetOverrides
            </Types>
        """.trimIndent()
    }

    private fun rootRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun workbookXml(): String {
        val sheetTags = sheets.mapIndexed { index, sheet ->
            """<sheet name="${xmlEscape(sheet.name)}" sheetId="${index + 1}" r:id="rId${index + 1}"/>"""
        }.joinToString("\n    ")
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                      xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets>
                $sheetTags
              </sheets>
            </workbook>
        """.trimIndent()
    }

    private fun workbookRelsXml(): String {
        val sheetRels = sheets.indices.joinToString("\n  ") { i ->
            """<Relationship Id="rId${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${i + 1}.xml"/>"""
        }
        val stylesId = sheets.size + 1
        val stringsId = sheets.size + 2
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              $sheetRels
              <Relationship Id="rId$stylesId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
              <Relationship Id="rId$stringsId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
            </Relationships>
        """.trimIndent()
    }

    class Sheet internal constructor(
        val name: String,
        private val columns: List<Column>,
        private val autoFilter: Boolean,
        private val rightToLeft: Boolean
    ) {
        private var rows = mutableListOf<List<Cell>>()

        fun addHeader(vararg values: String) {
            rows.add(values.map { Cell.Text(it, STYLE_HEADER) })
        }

        fun addRow(vararg cells: Cell) {
            rows.add(cells.toList())
        }

        fun addBlankRow() {
            rows.add(emptyList<Cell>())
        }

        internal fun toXml(shared: SharedStrings): String {
            val lastCol = (rows.maxOfOrNull { it.size } ?: 1).coerceAtLeast(1)
            val lastRow = rows.size.coerceAtLeast(1)
            val dimension = "A1:${colName(lastCol - 1)}$lastRow"
            val colXml = if (columns.isEmpty()) {
                ""
            } else {
                buildString {
                    append("<cols>")
                    columns.forEach { col ->
                        append("""<col min="${col.index}" max="${col.index}" width="${col.width}" customWidth="1"/>""")
                    }
                    append("</cols>")
                }
            }
            val rtlAttr = if (rightToLeft) """ rightToLeft="1"""" else ""
            val filterXml = if (autoFilter && rows.isNotEmpty()) {
                """<autoFilter ref="A1:${colName(lastCol - 1)}1"/>"""
            } else {
                ""
            }
            val data = buildString {
                rows.forEachIndexed { rowIndex, cells ->
                    val r = rowIndex + 1
                    val height = if (rowIndex == 0) """ ht="20" customHeight="1"""" else ""
                    append("""<row r="$r"$height>""")
                    cells.forEachIndexed { colIndex, cell ->
                        append(cell.toXml(colName(colIndex) + r, shared))
                    }
                    append("</row>")
                }
            }
            return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <dimension ref="$dimension"/>
                  <sheetViews>
                    <sheetView workbookViewId="0"$rtlAttr>
                      <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
                    </sheetView>
                  </sheetViews>
                  <sheetFormatPr defaultRowHeight="15"/>
                  $colXml
                  <sheetData>$data</sheetData>
                  $filterXml
                </worksheet>
            """.trimIndent()
        }
    }

    data class Column(val index: Int, val width: Double)

    sealed class Cell {
        abstract fun toXml(ref: String, shared: SharedStrings): String

        data class Text(val value: String, val style: Int = STYLE_TEXT) : Cell() {
            override fun toXml(ref: String, shared: SharedStrings): String {
                val idx = shared.id(value)
                return """<c r="$ref" s="$style" t="s"><v>$idx</v></c>"""
            }
        }

        data class Number(val value: Double, val style: Int = STYLE_NUMBER) : Cell() {
            override fun toXml(ref: String, shared: SharedStrings): String {
                val plain = formatPlainNumber(value)
                return """<c r="$ref" s="$style"><v>$plain</v></c>"""
            }
        }
    }

    internal class SharedStrings {
        private val index = LinkedHashMap<String, Int>()
        private var occurrences = 0

        fun id(value: String): Int {
            occurrences++
            return index.getOrPut(value) { index.size }
        }

        fun toXml(): String {
            val items = buildString {
                index.keys.forEach { value ->
                    val space = if (value.startsWith(' ') || value.endsWith(' ')) {
                        """ xml:space="preserve""""
                    } else {
                        ""
                    }
                    append("<si><t$space>${xmlEscape(value)}</t></si>")
                }
            }
            return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="$occurrences" uniqueCount="${index.size}">
                  $items
                </sst>
            """.trimIndent()
        }
    }

    companion object {
        const val STYLE_TEXT = 2
        const val STYLE_HEADER = 1
        const val STYLE_NUMBER = 3
        const val STYLE_TOTAL = 4

        private val INVALID_SHEET_CHARS = Regex("""[\\/:?*\[\]]""")

        private fun sanitizeSheetName(name: String): String {
            val cleaned = INVALID_SHEET_CHARS.replace(name, " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .trimEnd('\'')
            return cleaned.take(31).ifEmpty { "Sheet" }
        }

        private fun colName(index: Int): String {
            var n = index
            val sb = StringBuilder()
            while (n >= 0) {
                sb.insert(0, ('A' + n % 26))
                n = n / 26 - 1
            }
            return sb.toString()
        }

        private fun formatPlainNumber(value: Double): String {
            if (!value.isFinite()) return "0"
            return if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                value.toString()
            }
        }

        private fun xmlEscape(value: String): String =
            buildString(value.length) {
                for (c in value) {
                    when (c) {
                        '&' -> append("&amp;")
                        '<' -> append("&lt;")
                        '>' -> append("&gt;")
                        '"' -> append("&quot;")
                        '\'' -> append("&apos;")
                        in '\u0000'..'\u0008', '\u000B', '\u000C', in '\u000E'..'\u001F' -> Unit
                        else -> append(c)
                    }
                }
            }

        private fun ZipOutputStream.putText(path: String, content: String) {
            putNextEntry(ZipEntry(path))
            write(content.toByteArray(Charsets.UTF_8))
            closeEntry()
        }

        /**
         * Required fills: none + gray125. Extra fills/fonts used for header and amounts.
         * numFmtId 3 is the built-in #,##0 format.
         */
        private val STYLES_XML = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <fonts count="3">
                <font><sz val="11"/><color theme="1"/><name val="Calibri"/><family val="2"/></font>
                <font><b/><sz val="11"/><color theme="1"/><name val="Calibri"/><family val="2"/></font>
                <font><sz val="11"/><color rgb="FF000000"/><name val="Calibri"/><family val="2"/></font>
              </fonts>
              <fills count="2">
                <fill><patternFill patternType="none"/></fill>
                <fill><patternFill patternType="gray125"/></fill>
              </fills>
              <borders count="1">
                <border><left/><right/><top/><bottom/><diagonal/></border>
              </borders>
              <cellStyleXfs count="1">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
              </cellStyleXfs>
              <cellXfs count="5">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="3" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
                <xf numFmtId="3" fontId="1" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1"/>
              </cellXfs>
              <cellStyles count="1">
                <cellStyle name="Normal" xfId="0" builtinId="0"/>
              </cellStyles>
              <dxfs count="0"/>
              <tableStyles count="0" defaultTableStyle="TableStyleMedium2" defaultPivotStyle="PivotStyleLight16"/>
            </styleSheet>
        """.trimIndent()
    }
}
