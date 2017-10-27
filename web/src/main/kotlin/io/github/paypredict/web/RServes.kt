package io.github.paypredict.web

import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 10/28/2017.
 */

internal object RServes {
    fun open() {

    }

    fun close() {

    }
}

@WebListener("RServes WebListener")
class RServesWebListener : ServletContextListener {
    override fun contextInitialized(sce: ServletContextEvent) {
        RServes.open()
    }

    override fun contextDestroyed(sce: ServletContextEvent) {
        RServes.close()
    }
}