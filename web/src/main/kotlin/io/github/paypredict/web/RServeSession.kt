package io.github.paypredict.web

import io.github.paypredict.rserve.RServe
import org.rosuda.REngine.REXP
import org.rosuda.REngine.Rserve.RConnection
import org.rosuda.REngine.Rserve.RserveException
import java.io.File
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


abstract class RServeSession(private val rServe: RServe) {
    data class Status(val name: String, val error: Throwable? = null)
    data class CommandStatus(val status: Status, val scriptName: String, val result: REXP?, val error: Throwable?)

    var status = Status("initializing")
        private set(value) {
            field = value
            onStatusUpdated.forEach { it(this) }
        }
    val onStatusUpdated: MutableList<(RServeSession) -> Unit> = CopyOnWriteArrayList()

    private val actions: BlockingQueue<Action> = LinkedBlockingQueue()

    private val thread = kotlin.concurrent.thread(start = false, name = javaClass.name + " RServe thread") {
        try {
            status = Status("opening")
            onOpen(rServe)
            status = Status("opened")
        } catch (e: Throwable) {
            status = status.copy(error = e)
            return@thread
        }
        loop@
        while (true) {
            val action: Action? = actions.poll(5, TimeUnit.SECONDS)
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

    fun open() {
        thread.start()
    }

    fun shutdown() {
        actions.put(Shutdown)
    }

    fun join() {
        thread.join()
    }

    operator fun invoke(vararg path: String, map: (String) -> String = {it}, onFinish: (CommandStatus) -> Unit = {})
            = invoke(path.toList(), map, onFinish)

    operator fun invoke(path: Collection<String>, map: (String) -> String = {it}, onFinish: (CommandStatus) -> Unit = {}) {
        actions.put(Command {
            val scriptName = name + path.joinToString(prefix = "/", separator = "/")
            try {
                status = Status("running $scriptName")
                val script = map(homeDir.resolve(path).readText().replace("\r", ""))
                val rexp: REXP? = eval(script)
                status = Status("script $scriptName finished")
                when (rexp) {
                    null -> CommandStatus(status, scriptName, null, RserveException(this, lastError))
                    else -> CommandStatus(status, scriptName, rexp, null)
                }
            } catch (e: Throwable) {
                status = Status("script $scriptName error", e)
                CommandStatus(status, scriptName, null, e)
            }.let(onFinish)
        })
    }

    open fun onOpen(rServe: RServe) {}

    open fun onShutdown(rServe: RServe) {}

    protected val name = javaClass.simpleName
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