package com.slocator.fleetdriver.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * A minimal, dependency-free XLSX reader.
 *
 * Why custom rather than Apache POI / fastexcel?
 *  - Apache POI pulls in tens of MB of jars and bundles classes (javax.xml.*) that
 *    don't ship with Android, leading to sticky DEX issues.
 *  - We only need to read text-cell values from a small workbook. The XLSX
 *    format is plainly a ZIP of XML files, and the subset we need is tiny.
 *
 * What this parser supports:
 *  - Sheet enumeration (workbook.xml + workbook.xml.rels) — the sheet name is treated
 *    as the driver's identifier (phone number).
 *  - Shared strings (xl/sharedStrings.xml) — most XLSX text values land here.
 *  - Inline strings (<c t="inlineStr"><is><t>...</t></is></c>).
 *  - String values typed as t="str" or t="s" (shared-string index).
 *
 * What this parser does NOT support (intentionally):
 *  - Numeric formatting, dates, formulas. Our cells are URL strings + day labels,
 *    so this is fine.
 */
object XlsxParser {

    data class Sheet(val name: String, val rows: List<List<String>>)
    data class Workbook(val sheets: List<Sheet>)

    fun parse(input: InputStream): Workbook {
        // 1) Slurp the zip into memory: name -> bytes. XLSX files are small (tens of KB).
        val entries = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (!entry.isDirectory) {
                    entries[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
            }
        }

        // 2) Shared strings (optional file).
        val sharedStrings: List<String> =
            entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()

        // 3) Sheet name -> internal r:id from xl/workbook.xml.
        val sheetMeta = parseSheetIndex(
            entries["xl/workbook.xml"] ?: error("workbook.xml missing — not a valid XLSX")
        )

        // 4) r:id -> target path from xl/_rels/workbook.xml.rels.
        val relTargets = parseRelTargets(
            entries["xl/_rels/workbook.xml.rels"]
                ?: error("workbook.xml.rels missing — not a valid XLSX")
        )

        // 5) For each sheet, locate worksheet xml & parse rows.
        val sheets = sheetMeta.map { (name, rid) ->
            val target = relTargets[rid]
                ?: error("Workbook references rId=$rid but no rels target found.")
            // Targets are relative to xl/, normalize.
            val key = if (target.startsWith("/")) target.removePrefix("/")
            else "xl/$target"
            val xml = entries[key]
                ?: entries[key.removePrefix("xl/")]
                ?: error("Worksheet xml not found at $key")
            Sheet(name = name, rows = parseSheet(xml, sharedStrings))
        }

        return Workbook(sheets)
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val out = ArrayList<String>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(bytes.inputStream(), null)

        var current = StringBuilder()
        var inSi = false
        var inT = false

        var ev = parser.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> { inSi = true; current = StringBuilder() }
                        "t" -> if (inSi) inT = true
                    }
                }
                XmlPullParser.TEXT -> if (inT) current.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inT = false
                    "si" -> { out.add(current.toString()); inSi = false }
                }
            }
            ev = parser.next()
        }
        return out
    }

    private fun parseSheetIndex(bytes: ByteArray): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(bytes.inputStream(), null)

        var ev = parser.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG && parser.name == "sheet") {
                var name: String? = null
                var rid: String? = null
                for (i in 0 until parser.attributeCount) {
                    val attr = parser.getAttributeName(i)
                    val value = parser.getAttributeValue(i)
                    when {
                        attr == "name" -> name = value
                        attr.endsWith("id", ignoreCase = true) && attr.contains("r", true) -> rid = value
                    }
                }
                if (name != null && rid != null) out.add(name to rid)
            }
            ev = parser.next()
        }
        return out
    }

    private fun parseRelTargets(bytes: ByteArray): Map<String, String> {
        val out = HashMap<String, String>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(bytes.inputStream(), null)

        var ev = parser.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG && parser.name == "Relationship") {
                var id: String? = null
                var target: String? = null
                for (i in 0 until parser.attributeCount) {
                    when (parser.getAttributeName(i)) {
                        "Id" -> id = parser.getAttributeValue(i)
                        "Target" -> target = parser.getAttributeValue(i)
                    }
                }
                if (id != null && target != null) out[id] = target
            }
            ev = parser.next()
        }
        return out
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = ArrayList<List<String>>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(bytes.inputStream(), null)

        var currentRow: MutableList<String>? = null
        var cellRef: String? = null
        var cellType: String? = null
        var cellText: StringBuilder? = null
        var inV = false
        var inT = false

        var ev = parser.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> currentRow = ArrayList()
                    "c" -> {
                        cellRef = parser.getAttributeValue(null, "r")
                        cellType = parser.getAttributeValue(null, "t")
                        cellText = StringBuilder()
                    }
                    "v" -> inV = true
                    "t" -> inT = true
                }
                XmlPullParser.TEXT -> if (inV || inT) cellText?.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> inV = false
                    "t" -> inT = false
                    "c" -> {
                        val raw = cellText?.toString().orEmpty()
                        val resolved: String = when (cellType) {
                            "s" -> {
                                val idx = raw.toIntOrNull()
                                if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                            }
                            // Inline strings: text was captured directly.
                            "inlineStr", "str" -> raw
                            // Boolean
                            "b" -> if (raw == "1") "TRUE" else "FALSE"
                            else -> raw
                        }
                        // Place into current row at the column index decoded from r="A1".
                        val colIdx = columnIndexFromRef(cellRef)
                        if (currentRow != null && colIdx >= 0) {
                            while (currentRow.size <= colIdx) currentRow.add("")
                            currentRow[colIdx] = resolved
                        }
                        cellRef = null; cellType = null; cellText = null
                    }
                    "row" -> {
                        currentRow?.let { rows.add(it) }
                        currentRow = null
                    }
                }
            }
            ev = parser.next()
        }
        return rows
    }

    /**
     * Convert "A1", "AB17", etc. to a 0-based column index.
     * Strips the trailing row digits.
     */
    private fun columnIndexFromRef(ref: String?): Int {
        if (ref.isNullOrEmpty()) return -1
        var col = 0
        for (ch in ref) {
            if (ch.isLetter()) {
                col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else break
        }
        return col - 1
    }
}
