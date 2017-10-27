package io.github.paypredict.web.ui

import com.vaadin.annotations.VaadinServletConfiguration
import com.vaadin.server.VaadinRequest
import com.vaadin.server.VaadinServlet
import com.vaadin.ui.UI
import javax.servlet.annotation.WebServlet

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 10/28/2017.
 */

@WebServlet(urlPatterns = arrayOf("/pp/*", "/VAADIN/*"), name = "PPUIServlet", asyncSupported = true)
@VaadinServletConfiguration(ui = PayPredictUI::class, productionMode = false)
class PPUIServlet : VaadinServlet()

internal class PayPredictUI : UI() {
    override fun init(p0: VaadinRequest) {

    }

}