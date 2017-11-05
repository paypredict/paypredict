package io.github.paypredict.web.ui

import com.vaadin.icons.VaadinIcons
import com.vaadin.server.ExternalResource
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme
import io.github.paypredict.web.CPT
import io.github.paypredict.web.Payer
import io.github.paypredict.web.RSS
import io.github.paypredict.web.RServeSession
import org.rosuda.REngine.REXP

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 11/04/2017.
 */
class DownloadCptLinesWithNoEobDC : VerticalLayout() {
    private val onStatusUpdated: (RServeSession) -> Unit

    init {
        caption = "Download CPT Lines with no EOB"
        setMargin(true)
        setWidth("32em")
        setHeightUndefined()
        val status = Label().apply {
            setWidth("100%")
            addStyleName(ValoTheme.LABEL_COLORED)
        }
        val payer = ComboBox<Payer>("Payer").apply {
            setWidth("100%")
            isEnabled = false
        }

        val links = VerticalLayout().apply {
            setSizeUndefined()
        }

        addComponent(HorizontalLayout().apply {
            setWidth("100%")
            addComponentsAndExpand(payer)
            val button = Button("Build").apply {
                isEnabled = false
                isDisableOnClick = true
                addClickListener { event ->
                    val cpt = payer.value
                    RSS.downloadCptLinesWithNoEob.buildCSV(cpt.code) { cmd, url ->
                        event.button.isEnabled = true
                        cmd.showResult {
                            links.addComponent(Link("$cpt.csv", ExternalResource(url)).apply {
                                icon = VaadinIcons.EXTERNAL_LINK
                                targetName = "_blank"
                            }, 0)
                        }
                    }
                }
            }
            payer.addSelectionListener {
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

        RSS.downloadCptLinesWithNoEob.payerItems  { cmd, items ->
            cmd.showResult {
                payer.setItems(items)
                payer.isEnabled = true
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
        RSS.downloadCptLinesWithNoEob.onStatusUpdated += onStatusUpdated
        onStatusUpdated(RSS.downloadCptLinesWithNoEob)
    }

    override fun detach() {
        RSS.downloadCptLinesWithNoEob.onStatusUpdated -= onStatusUpdated
        super.detach()
    }
}