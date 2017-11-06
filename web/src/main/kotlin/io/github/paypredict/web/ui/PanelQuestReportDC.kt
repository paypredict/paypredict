package io.github.paypredict.web.ui

import com.vaadin.icons.VaadinIcons
import com.vaadin.server.ExternalResource
import com.vaadin.shared.ui.ContentMode
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme
import io.github.paypredict.web.CPT
import io.github.paypredict.web.RSS
import io.github.paypredict.web.RServeSession
import org.rosuda.REngine.REXP

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 10/28/2017.
 */
class PanelQuestReportDC : VerticalLayout() {
    private val rss = RSS.panelQuestReport
    private val onStatusUpdated: (RServeSession) -> Unit

    init {
        val conf = rss.conf()
        caption = conf["title"] as? String ?: "Panel Quest Report"
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

        val links = VerticalLayout().apply {
            setSizeUndefined()
        }

        (conf["description"] as? String)?.let {
            addComponent(Label(it, ContentMode.HTML))
        }

        addComponent(HorizontalLayout().apply {
            setWidth("100%")
            addComponentsAndExpand(cptCode)
            val button = Button("Build").apply {
                isEnabled = false
                isDisableOnClick = true
                addClickListener { event ->
                    val cpt = cptCode.value
                    rss.buildReport(cpt.code) { cmd, url ->
                        event.button.isEnabled = true
                        cmd.showResult {
                            links.addComponent(Link("$cpt Report", ExternalResource(url)).apply {
                                icon = VaadinIcons.EXTERNAL_LINK
                                targetName = "_blank"
                            }, 0)
                        }
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

        addComponent(links)

        onStatusUpdated = {
            ui.access {
                status.value = it.status.name
            }
        }

        rss.cptItems { cmd, cptItems ->
            cmd.showResult {
                cptCode.setItems(cptItems)
                cptCode.isEnabled = true
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
        rss.onStatusUpdated += onStatusUpdated
        onStatusUpdated(rss)
    }

    override fun detach() {
        rss.onStatusUpdated -= onStatusUpdated
        super.detach()
    }
}
