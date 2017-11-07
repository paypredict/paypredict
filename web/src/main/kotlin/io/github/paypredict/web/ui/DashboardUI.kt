package io.github.paypredict.web.ui

import com.vaadin.annotations.Push
import com.vaadin.annotations.Title
import com.vaadin.annotations.VaadinServletConfiguration
import com.vaadin.icons.VaadinIcons
import com.vaadin.server.ExternalResource
import com.vaadin.server.VaadinRequest
import com.vaadin.server.VaadinServlet
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme
import io.github.paypredict.web.initWindowTopToolbar
import io.github.paypredict.web.initWithLogin
import javax.servlet.annotation.WebServlet

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 10/28/2017.
 */

private const val UI_CAPTION = "PayPredict Dashboard"

@WebServlet(urlPatterns = arrayOf("/dashboard/*"), name = "DashboardUIServlet", asyncSupported = true)
@VaadinServletConfiguration(ui = DashboardUI::class, productionMode = false)
class PPUIServlet : VaadinServlet()

@Title(UI_CAPTION)
@Push
internal class DashboardUI : UI() {
    private val dcMap = mapOf<String, () -> DashboardComponent>(
            PanelQuestReportDC.name to { PanelQuestReportDC() },
            DownloadCptLinesWithNoEobDC.name to { DownloadCptLinesWithNoEobDC() }
    )

    override fun init(request: VaadinRequest) {
        initWithLogin(UI_CAPTION) {
            VerticalLayout().apply {
                setSizeFull()
                val dc = page.uriFragment?.let { dcMap[it] }?.invoke()
                val title = dc?.title ?: UI_CAPTION
                page.setTitle(title)
                initWindowTopToolbar(title)

                addComponentsAndExpand(Panel().apply {
                    setSizeFull()
                    addStyleName(ValoTheme.PANEL_BORDERLESS)

                    content = dc?.component ?: HorizontalLayout().apply {
                        setSizeUndefined()
                        setMargin(true)
                        addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING)
                        dcMap.values.forEach {
                            addComponent(it().toDashboardComponent())
                        }
                    }
                })
            }
        }
    }

    private fun DashboardComponent.toDashboardComponent(): Component = VerticalLayout().apply {
        setSizeUndefined()
        addStyleName(ValoTheme.LAYOUT_CARD)
        setMargin(false)

        addComponent(HorizontalLayout().apply {
            setWidth("100%")
            addStyleName("v-panel-caption")

            addComponentsAndExpand(Label(title))
            addComponent(Link().apply {
                icon = VaadinIcons.EXTERNAL_LINK
                resource = ExternalResource("#$name")
                targetName = "_blank"
            })
        })
        addComponent(VerticalLayout().apply {
            setSizeUndefined()
            setMargin(false)
            addComponent(component)
        })
    }
}

