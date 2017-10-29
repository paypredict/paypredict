package io.github.paypredict.web.ui

import com.vaadin.annotations.Push
import com.vaadin.annotations.Title
import com.vaadin.annotations.VaadinServletConfiguration
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
    override fun init(request: VaadinRequest) {
        initWithLogin(UI_CAPTION) {
            VerticalLayout().apply {
                setSizeFull()
                initWindowTopToolbar(UI_CAPTION)

                addComponentsAndExpand(Panel().apply {
                    setSizeFull()
                    addStyleName(ValoTheme.PANEL_BORDERLESS)

                    content = HorizontalLayout().apply {
                        setSizeUndefined()
                        setMargin(true)
                        addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING)
                        addComponent(PanelQuestReportDC().toDashboardComponent())
                    }
                })
            }
        }
    }

    private fun Component.toDashboardComponent(): Component = Panel(caption).also {
        it.setSizeUndefined()
        it.content = this
    }
}

