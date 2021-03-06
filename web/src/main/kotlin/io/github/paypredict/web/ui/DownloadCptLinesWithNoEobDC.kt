package io.github.paypredict.web.ui

import com.vaadin.icons.VaadinIcons
import com.vaadin.server.ExternalResource
import com.vaadin.shared.ui.ContentMode
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme
import io.github.paypredict.web.Payer
import io.github.paypredict.web.RSS
import io.github.paypredict.web.RServeSession
import org.rosuda.REngine.REXP
import java.net.URL


/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 11/04/2017.
 */
class DownloadCptLinesWithNoEobDC : DashboardComponent, VerticalLayout() {
    companion object DC {
        private val rss = RSS.downloadCptLinesWithNoEob
        val name: String = rss.javaClass.simpleName
    }

    private val onStatusUpdated: (RServeSession) -> Unit

    override val name: String = DC.name
    override val title: String by lazy { rss.conf()["title"] as? String ?: "Panel Quest Report"  }
    override val component: Component = this

    init {
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


        (rss.conf()["description"] as? String)?.let {
            addComponent(Label(it, ContentMode.HTML))
        }

        addComponent(HorizontalLayout().apply {
            setWidth("100%")
            addComponentsAndExpand(payer)
            val button = Button("Build").apply {
                isEnabled = false
                isDisableOnClick = true
                addClickListener { event ->
                    val payer1 = payer.value
                    rss.buildCSV(payer1) { cmd, url ->
                        event.button.isEnabled = true
                        cmd.showResult {
                            links.addComponent(Link().apply {
                                icon = VaadinIcons.EXTERNAL_LINK
                                targetName = "_blank"
                                caption = url?.path?.split("/")?.last() ?: "?"
                                resource = ExternalResource(url?.toURL()?: URL("#"))
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

        rss.payerItems  { cmd, items ->
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
        rss.onStatusUpdated += onStatusUpdated
        onStatusUpdated(rss)
    }

    override fun detach() {
        rss.onStatusUpdated -= onStatusUpdated
        super.detach()
    }
}