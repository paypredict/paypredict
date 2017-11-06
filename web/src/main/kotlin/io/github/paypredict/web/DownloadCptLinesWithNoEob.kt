package io.github.paypredict.web

import io.github.paypredict.rserve.RServe
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.util.*


/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 11/4/2017.
 */
class DownloadCptLinesWithNoEob(rServe: RServe) : RServeSession(rServe) {
    override fun onOpen(rServe: RServe) {
        rServe.exec("""
            dir_rss_scripts = "${homeDir.rPath}"
        """.trimIndent())
        invokeDir("init")
    }

    fun payerItems(onFinish: (CommandStatus, List<Payer>) -> Unit) {
        invoke("payerItems.R") { cmd ->
            val cptItems: List<Payer> = mutableListOf<Payer>().apply {
                val map = cmd.result?.asNativeJavaObject() as? Map<*, *>
                if (map != null) {
                    val codes = map["PAYOR_ID"] as? Array<*>
                    val names = map["search"] as? Array<*>
                    if (codes != null && names != null) {
                        codes.forEachIndexed { index, code ->
                            this += Payer(
                                    codes.getOrElse(index, { "?$it" }).toString(),
                                    names.getOrElse(index, { "?$it" }).toString())
                        }
                    }
                }
            }
            onFinish(cmd, cptItems)
        }
    }

    fun build(payer: Payer, onFinish: (cmd: CommandStatus, excelURL: String) -> Unit) {
        val tempDir = Files.createTempDirectory("rss.$name.").toFile()
        invoke("build.R", map = { script ->
            """
            ex_payer <- '${payer.code}'
            $script"""
        }) { cmd ->
            if (cmd.error == null) {
                val data = cmd.result?.asNativeJavaObject() as? Map<*, *>
                if (data == null) {
                    onFinish(cmd.copy(error = AssertionError("Invalid result")), "#")
                } else {
                    val workbook = XSSFWorkbook().apply {
                        createSheet().apply {
                            if (data.keys.isNotEmpty()) {
                                // header
                                createRow(0).apply {
                                    data.keys.forEachIndexed { index, value ->
                                        createCell(index).apply {
                                            setCellValue(value as? String)
                                        }
                                    }
                                }
                                // values
                                val rows = data.values.first().let {
                                    when (it) {
                                        is Array<*> -> it.size
                                        is DoubleArray -> it.size
                                        is IntArray -> it.size
                                        else -> 0
                                    }
                                }
                                for (row in 1..rows) {
                                    createRow(row).apply {
                                        data.keys.forEachIndexed { index, key ->
                                            createCell(index).apply {
                                                val value = data[key]?.let {
                                                    when (it) {
                                                        is Array<*> -> it.getOrNull(row - 1)
                                                        is DoubleArray -> it.getOrNull(row - 1)
                                                        is IntArray -> it.getOrNull(row - 1)
                                                        else -> null
                                                    }
                                                }
                                                when (value) {
                                                    null -> setCellValue(value as String?)
                                                    is Boolean -> setCellValue(value)
                                                    is String -> setCellValue(value)
                                                    is Date -> setCellValue(value)
                                                    is Number -> setCellValue(value.toDouble())
                                                    else -> setCellValue(value.toString())
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val workbookFile = tempDir.resolve("workbook.xlsx")
                    workbookFile.outputStream().use { workbook.write(it) }

                    val properties = loadProperties(name = "report.site.properties") {
                        setProperty("url", "http://localhost/rss/")
                        setProperty("root", File("/PayPredict/web/rss/").absolutePath)
                        true
                    }

                    val siteRoot = File(properties["root"] as String)
                    val siteURI = URI.create(properties["url"] as String)
                    val siteWorkbookName = "${payer.safeFileName}.xlsx"
                    workbookFile.copyTo(siteRoot.resolve(name).apply { mkdirs() }.resolve(siteWorkbookName), overwrite = true)
                    onFinish(cmd, siteURI.resolve(name + "/").resolve(siteWorkbookName).toASCIIString())
                }
            } else {
                onFinish(cmd, "#")
            }
        }
    }

}