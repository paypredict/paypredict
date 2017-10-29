package io.github.paypredict.web.ui

import com.vaadin.icons.VaadinIcons
import com.vaadin.server.ExternalResource
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme
import io.github.paypredict.web.RSS
import io.github.paypredict.web.RServeSession
import org.rosuda.REngine.REXP

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 10/28/2017.
 */
class PanelQuestReportDC : VerticalLayout() {
    private val onStatusUpdated: (RServeSession) -> Unit
    private val onNewReport: (CPT, REXP) -> Unit

    private class CPT(val code: String, val name: String) {
        override fun toString(): String = "$code | $name"
    }

    init {
        caption = "Panel Quest Report"
        setMargin(true)
        setWidth("32em")
        setHeightUndefined()
        val status = Label().apply {
            setWidth("100%")
            addStyleName(ValoTheme.LABEL_COLORED)
        }
        val cptCode = ComboBox<CPT>("CPT Code").apply {
            setWidth("100%")
            isEnabled = false
        }
        onNewReport = { cpt, rexp ->
            val reportPath = rexp.asString()
            Notification.show(reportPath, Notification.Type.TRAY_NOTIFICATION)
            addComponent(Link("$cpt Report", ExternalResource("#report:$reportPath")).apply {
                icon = VaadinIcons.EXTERNAL_LINK
                targetName = "_blank"
            })
        }
        addComponent(HorizontalLayout().apply {
            setWidth("100%")
            addComponentsAndExpand(cptCode)
            val button = Button("Build").apply {
                isEnabled = false
                isDisableOnClick = true
                addClickListener { event ->
                    val cpt = cptCode.value
                    RSS.panelQuestReport.buildReport(cpt.code) { cmd ->
                        event.button.isEnabled = true
                        cmd.showResult { onNewReport(cpt, it) }
                    }
                }
            }
            cptCode.addSelectionListener {
                button.isEnabled = it.value != null
            }
            addComponent(button)
            setComponentAlignment(button, Alignment.BOTTOM_LEFT)
        })
        addComponent(HorizontalLayout().apply {
            setWidth("100%")
            addComponentsAndExpand(status)
        })

        onStatusUpdated = {
            ui.access {
                status.value = it.status.name
            }
        }

        RSS.panelQuestReport.cptItems {
            it.showResult {
                val result = it.asNativeJavaObject() as? List<*>
                if (result != null) {
                    cptCode.setItems(result.mapNotNull {
                        (it as? Array<*>)?.let {
                            if (it.size >= 2) CPT(it[0].toString(), it[1].toString()) else null
                        }
                    }.toList())
                    cptCode.isEnabled = true
                } else {
                    Notification.show("Invalid RSS.panelQuestReport.cptItems result: "
                            + it.asNativeJavaObject(), Notification.Type.WARNING_MESSAGE)
                }
            }
        }

    }

    private fun RServeSession.CommandStatus.showResult(onSuccess: (REXP) -> Unit) {
        ui.access {
            when {
                error != null -> Notification.show(error.message, Notification.Type.WARNING_MESSAGE)
                result != null -> onSuccess(result)
            }
        }
    }


    override fun attach() {
        super.attach()
        RSS.panelQuestReport.onStatusUpdated += onStatusUpdated
        onStatusUpdated(RSS.panelQuestReport)
    }

    override fun detach() {
        RSS.panelQuestReport.onStatusUpdated -= onStatusUpdated
        super.detach()
    }
}
