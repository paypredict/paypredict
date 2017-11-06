package io.github.paypredict.web

import io.github.paypredict.rserve.RServe
import org.rosuda.REngine.REXP
import org.rosuda.REngine.Rserve.RConnection
import org.rosuda.REngine.Rserve.RserveException
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 10/28/2017.
 */

private sealed class Action

private class Command(val command: RConnection.() -> Unit) : Action()

private object Shutdown : Action()

internal fun String.rScript() = replace("\r", "")

class ScheduledAction(val period: Duration, val path: Collection<String>) {
    override fun toString() = path.joinToString(separator = "/")

    private var lastTime: Instant? = null
    fun isTriggered(now: Instant): Boolean = lastTime.let {
        when (it) {
            null -> {
                lastTime = now
                false
            }
            else ->
                now.isAfter(it + period).also {
                    if (it) lastTime = now
                }
        }
    }
}

abstract class RServeSession(private val rServe: RServe) {
    data class Status(val name: String, val error: Throwable? = null)
    data class CommandStatus(val status: Status, val scriptName: String, val result: REXP?, val error: Throwable?)

    fun conf(): Map<String, Any> = homeDir.resolve("conf.yml").let {
        when {
            it.isFile -> it.inputStream().use { Yaml().load<Map<String, Any>>(it) }
            else -> emptyMap()
        }
    }

    var status = Status("initializing")
        private set(value) {
            field = value
            onStatusUpdated.forEach { it(this) }
        }
    val onStatusUpdated: MutableList<(RServeSession) -> Unit> = CopyOnWriteArrayList()

    private val actions: BlockingQueue<Action> = LinkedBlockingQueue()
    protected val scheduledActions: MutableList<ScheduledAction> = CopyOnWriteArrayList()

    private val thread = kotlin.concurrent.thread(start = false, name = javaClass.name + " RServe thread") {
        try {
            status = Status("opening $name")
            onOpen(rServe)
            status = Status("$name was opened")
        } catch (e: Throwable) {
            status = status.copy(error = e)
            return@thread
        }
        loop@
        while (true) {
            val action: Action? = actions.poll(1, TimeUnit.SECONDS) ?: nextScheduledAction()
            when (action) {
                null -> continue@loop
                is Shutdown -> break@loop
                is Command -> rServe.command(action.command)
            }
        }
        status = Status("running onShutdown(rServe)")
        onShutdown(rServe)
        rServe.shutdown()
    }

    private fun nextScheduledAction(): Command? = scheduledActions
            .firstOrNull { it.isTriggered(Instant.now()) }
            ?.let { action ->
                Command {
                    val actionName = name + "/" + action
                    status = Status("running $actionName")
                    status = try {
                        val script = homeDir.resolve(action.path).readText().rScript()
                        val rexp = eval(script)
                        Status(when {
                            rexp.isString -> "$actionName done: ${rexp.asString()}"
                            else -> "$actionName done"
                        })
                    } catch (e: Throwable) {
                        Status(name = "error on " + status.name, error = e)
                    }
                }
            }

    fun open() {
        thread.start()
    }

    fun shutdown() {
        actions.put(Shutdown)
    }

    fun join() {
        thread.join()
    }

    operator fun invoke(vararg path: String, map: (String) -> String = { it }, action: (CommandStatus) -> Unit = {})
            = invoke(path.toList(), map, action)

    operator fun invoke(path: Collection<String>, map: (String) -> String = { it }, action: (CommandStatus) -> Unit = {}) {
        actions.put(Command {
            val scriptName = name + path.joinToString(prefix = "/", separator = "/")
            val commandStatus = try {
                status = Status("running $scriptName")
                val script = map(homeDir.resolve(path).readText().rScript()).rScript()
                val rexp: REXP? = eval(script)
                status = Status("script $scriptName finished")
                when (rexp) {
                    null -> CommandStatus(status, scriptName, null, RserveException(this, lastError))
                    else -> CommandStatus(status, scriptName, rexp, null)
                }
            } catch (e: Throwable) {
                status = Status("script $scriptName error", e)
                CommandStatus(status, scriptName, null, e)
            }
            try {
                action(commandStatus)
            } catch (e: Throwable) {
                status = Status("script $scriptName action error", e)
            }
        })
    }

    open fun onOpen(rServe: RServe) {}

    open fun onShutdown(rServe: RServe) {}

    protected val name: String = javaClass.simpleName
    protected val homeDir: File = payPredictHome.resolve("rss").resolve(name)

    private fun File.resolve(path: Collection<String>): File {
        var result = this
        path.forEach { result = result.resolve(it) }
        return result
    }

    protected fun invokeDir(vararg path: String) {
        homeDir.resolve(path.toList())
                .listFiles { _, name -> name.toUpperCase().endsWith(".R") }
                ?.sortedBy { it.name }
                ?.forEach {
                    invoke(path.toList() + it.name)
                }
    }

}