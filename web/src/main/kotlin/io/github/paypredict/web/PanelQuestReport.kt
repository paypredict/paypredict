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

    fun cptItems(onFinish: (CommandStatus) -> Unit) {
        invoke("cptItems.R", onFinish = onFinish)
    }

    fun buildReport(cptCode: String, onFinish: (CommandStatus) -> Unit) {
        invoke("buildReport.R", map = { "report.cpt.code <- '$cptCode'\n$it" }, onFinish = onFinish)
    }
}
