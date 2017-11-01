package io.github.paypredict.web

import com.vaadin.event.ShortcutAction
import com.vaadin.server.Sizeable
import com.vaadin.server.VaadinServlet
import com.vaadin.server.VaadinSession
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme
import java.io.File
import java.util.*
import javax.servlet.annotation.WebServlet
import kotlin.concurrent.thread

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 10/28/2017.
 */
@WebServlet(urlPatterns = arrayOf("/VAADIN/*"), name = "UI-Servlet", asyncSupported = true)
class UIServlet : VaadinServlet()

fun UI.initWithLogin(caption: String, createContent: () -> Component) {
    val session: VaadinSession = VaadinSession.getCurrent() ?: throw AssertionError("Invalid context")
    if (session.getAttribute(sessionLoginAttrName) != null) {
        content = createContent()
        return
    }

    content = VerticalLayout().apply {
        initWindowTopToolbar(caption)
        val loginLayout = HorizontalLayout().apply {
            setMargin(true)
            val userName = TextField("User")
            val password = PasswordField("Password")
            val progress = ProgressBar().apply {
                isVisible = false
                isIndeterminate = true
            }
            val button = Button("Login").apply {
                addStyleName(ValoTheme.BUTTON_PRIMARY)
                setClickShortcut(ShortcutAction.KeyCode.ENTER)
                isDisableOnClick = true
                fun setProgressMode(progressMode: Boolean) {
                    progress.isVisible = progressMode
                    isVisible = !progressMode
                    isEnabled = !progressMode
                    userName.isEnabled = !progressMode
                    password.isEnabled = !progressMode
                }
                addClickListener {
                    setProgressMode(true)
                    if (userName.value == properties["user"] && password.value == properties["password"]) {
                        session.setAttribute(sessionLoginAttrName, true)
                        content = createContent()
                    } else {
                        val ui = ui
                        thread(start = true) {
                            Thread.sleep(Random().nextInt(2000) + 1000L)
                            ui.access {
                                setProgressMode(false)
                                Notification.show("Invalid user or password", Notification.Type.WARNING_MESSAGE
                                )
                            }
                        }
                    }
                }
            }
            addComponents(userName, password, progress, button)
            setComponentAlignment(progress, Alignment.BOTTOM_LEFT)
            setComponentAlignment(button, Alignment.BOTTOM_LEFT)
            password.focus()
        }
        addComponents(loginLayout)
        setComponentAlignment(loginLayout, Alignment.TOP_CENTER)
    }
}

fun VerticalLayout.initWindowTopToolbar(caption: String, init: VerticalLayout.() -> Unit = {}) {
    setMargin(false)
    isSpacing = false
    addComponent(newWindowTopToolbar(caption))
    init()
}

fun newWindowTopToolbar(caption: String): HorizontalLayout = HorizontalLayout().apply {
    setWidth(100f, Sizeable.Unit.PERCENTAGE)
    addStyleName(ValoTheme.WINDOW_TOP_TOOLBAR)
    addComponent(Label(caption).apply {
        addStyleName(ValoTheme.LABEL_COLORED)
        addStyleName(ValoTheme.LABEL_LARGE)
        addStyleName(ValoTheme.LABEL_BOLD)
    })
}

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


private val sessionLoginAttrName = "PayPredict/user"

private val properties: Properties by lazy {
    loadProperties(name = "PayPredictUI.properties") {
        setProperty("user", "admin")
        setProperty("password", ByteArray(8).let {
            Random().nextBytes(it)
            it.joinToString(separator = "") { it.toString(Character.MAX_RADIX) }
        })
        true
    }
}
