package io.github.paypredict.web

import io.github.paypredict.rserve.RServe
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files


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

    fun buildCSV(payer: Payer, onFinish: (cmd: CommandStatus, csvURL: URI?) -> Unit) {
        val tempDir = Files.createTempDirectory("rss.$name.").toFile()
        invoke("buildCSV.R", map = { script ->
            """
            ex_payer <- '${payer.code}'
            ex_dir_output <- '${tempDir.rPath}'
            $script"""
        }) { cmd ->
            if (cmd.error == null) {
                val csvFileName = cmd.result!!.asString()
                try {
                    val properties = loadProperties(name = "report.site.properties") {
                        setProperty("url", "http://localhost/rss/")
                        setProperty("root", File("/PayPredict/web/rss/").absolutePath)
                        true
                    }

                    val siteRoot = File(properties["root"] as String)
                    val siteURI = URI.create(properties["url"] as String)
                    val csvFile = tempDir.resolve(csvFileName)
                    if (csvFile.parentFile != tempDir) throw IOException("Invalid csv file location: $csvFile")
                    if (csvFile.isFile.not()) throw IOException("csv file not found: $csvFile")
                    val siteCsvFile = siteRoot.resolve(name).apply { mkdirs() }.resolve(csvFile.name)
                    csvFile.copyTo(siteCsvFile, overwrite = true)

                    onFinish(cmd, siteURI.resolve(siteCsvFile.relativeTo(siteRoot).path.replace('\\', '/')))
                } catch (e: Throwable) {
                    onFinish(cmd.copy(error = e), null)
                }
            } else {
                onFinish(cmd, null)
            }
        }
    }
}