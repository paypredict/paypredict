package io.github.paypredict.web

import io.github.paypredict.rserve.RServe
import java.io.File
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Logger
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 10/28/2017.
 */

internal val payPredictHome: File by lazy {
    File(System.getenv("PAY_PREDICT_HOME") ?: "/PayPredict").apply {
        if (!exists()) mkdirs()
    }
}

internal fun loadProperties(dir: File = payPredictHome, name: String = "PayPredict.properties", putDefaults: Properties.() -> Boolean = { false }): Properties {
    val file = dir.resolve(name).normalize()
    return Properties().apply {
        if (file.isFile) {
            file.inputStream().use { load(it) }
        } else {
            if (putDefaults()) {
                file.outputStream().use { store(it, null) }
            }
        }
    }
}

internal val File.rPath: String
    get() = absolutePath.replace("\\", "/")

internal object RSS {
    private val log: Logger = Logger.getLogger(RSS.javaClass.name)
    private val lock = ReentrantLock()
    private val opened = mutableListOf<RServeSession>()
    private var isOpened = false
    val panelQuestReport: PanelQuestReport by lazy { openSession(6314) { PanelQuestReport(it) } }

    private inline fun <reified T : RServeSession> openSession(port: Int, create: (RServe) -> T): T = lock.withLock {
        if (!isOpened) throw AssertionError("Invalid RSS state")
        log.info("opening ${T::class.java.name}")
        create(RServe(port = port)).also {
            opened += it
            it.open()
        }
    }

    fun open() = lock.withLock {
        if (isOpened) throw AssertionError("Invalid RSS state")
        isOpened = true
    }

    fun close() = lock.withLock {
        preload?.let {
            it.interrupt()
            it.join()
        }
        if (isOpened) {
            opened.forEach { it.shutdown() }
            opened.forEach { it.join() }
        }
        isOpened = false
    }

    private var preload: Thread? = null

    fun preload() = lock.withLock {
        preload = preload ?: thread(start = true, name = "RSS.preload()") {
            Thread.sleep(3000)
            listOf(panelQuestReport).forEach {
                log.info("${it.javaClass.name}.status = ${it.status}")
            }
        }
    }
}

@WebListener("RSS WebListener")
class RServesWebListener : ServletContextListener {
    override fun contextInitialized(sce: ServletContextEvent) {
        RSS.open()
        RSS.preload()
    }

    override fun contextDestroyed(sce: ServletContextEvent) {
        RSS.close()
    }
}