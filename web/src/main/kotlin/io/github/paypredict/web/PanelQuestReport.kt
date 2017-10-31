package io.github.paypredict.web

import io.github.paypredict.rserve.RServe

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 10/28/2017.
 */
class PanelQuestReport(rServe: RServe) : RServeSession(rServe) {
    override fun onOpen(rServe: RServe) {
        invokeDir("init")
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

    fun buildReport(cptCode: String, onFinish: (CommandStatus) -> Unit) {
        invoke("buildReport.R", map = { "report.cpt.code <- '$cptCode'\n$it" }, onFinish = onFinish)
    }
}
