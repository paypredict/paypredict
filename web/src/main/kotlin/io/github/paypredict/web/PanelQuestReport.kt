package io.github.paypredict.web

import io.github.paypredict.rserve.RServe
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 10/28/2017.
 */
class PanelQuestReport(rServe: RServe) : RServeSession(rServe) {
    override fun onOpen(rServe: RServe) {
        rServe.exec("""
            dir_rss_scripts = "${homeDir.rPath}"
        """.trimIndent())
        invokeDir("init")
        initScheduler()
    }

    fun cptItems(onFinish: (CommandStatus, List<CPT>) -> Unit) {
        invoke("cptItems.R") { cmd ->
            val cptItems: List<CPT> = mutableListOf<CPT>().apply {
                val map = cmd.result?.asNativeJavaObject() as? Map<*, *>
                if (map != null) {
                    val codes = map["Cpt"] as? Array<*>
                    val names = map["dsc"] as? Array<*>
                    val inUse = map["inUse"] as? ByteArray
                    if (codes != null && names != null && inUse != null) {
                        inUse.forEachIndexed { index, isInUse ->
                            if (isInUse != 0.toByte()) {
                                this += CPT(
                                        codes.getOrElse(index, { "?$it" }).toString(),
                                        names.getOrElse(index, { "?$it" }).toString())
                            }
                        }
                    }
                }
            }
            onFinish(cmd, cptItems)
        }
    }


    fun buildReport(cptCode: String, onFinish: (cmd: CommandStatus, url: String) -> Unit) {
        val siteTempDir = Files.createTempDirectory("rss.report.site.").toFile()
        homeDir.resolve("report.site").copyRecursively(siteTempDir)
        invoke("buildReport.R", map = { script ->
            """
            report.cpt.code <- '$cptCode'
            report.site <- '${siteTempDir.rPath}'
            $script"""
        }) { cmd ->

            if (cmd.error == null) {
                val cmdReportFileName = cmd.result!!.asString()
                val properties = loadProperties(name = "report.site.properties") {
                    setProperty("url", "http://localhost/rss/")
                    setProperty("root", File("/PayPredict/web/rss/").absolutePath)
                    true
                }

                val siteRoot = File(properties["root"] as String)
                val siteURI = URI.create(properties["url"] as String)
                val siteOut = siteTempDir.resolve("out")
                if (siteOut.isDirectory) {
                    val reportName = "$name.$cptCode.html"
                    siteOut.copyRecursively(siteRoot, overwrite = false) { _, ioException ->
                        when (ioException) {
                            is FileAlreadyExistsException -> OnErrorAction.SKIP
                            else -> throw ioException
                        }
                    }
                    siteOut.resolve(cmdReportFileName).copyTo(siteRoot.resolve(reportName), overwrite = true)
                    onFinish(cmd, siteURI.resolve(reportName).toASCIIString())
                } else {
                    onFinish(cmd.copy(error = IOException("site out directory not found: $siteOut")), "#")
                }
            } else {
                onFinish(cmd, "#")
            }
        }
    }
}
