package io.github.paypredict.rserve

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 10/26/2017.
 */
object R {
    internal val binPath: String? by lazy {
        System.getProperty("R.binPath", null) ?: installPath?.let {
            File(it).resolve("bin").absolutePath
        }
    }

    internal val installPath: String? by lazy {
        fun windowsInstallPath(): String? = System.getProperty("R.installPath", null) ?: ProcessBuilder().run {
            command("reg", "query", "HKLM\\Software\\R-core\\R")
            val outFile = File.createTempFile("R.installPath.", ".tmp")
            try {
                redirectOutput(outFile)
                start().also { process ->
                    if (!process.waitFor(30, TimeUnit.SECONDS)) {
                        process.destroy()
                        throw IOException("${command()} timed out")
                    }
                }
                outFile.useLines {
                    for (line in it) {
                        val entry = line.trim().split("    REG_SZ    ")
                        if (entry.size == 2 && entry[0] == "InstallPath") return entry[1]
                    }
                }
            } finally {
                outFile.delete()
            }
            null
        }

        windowsInstallPath()
    }

    internal val libsUser: Set<String> by lazy {
        System.getProperty("R.libsUser", null)
                ?.split(";")
                ?.toSet()
                ?: emptySet()
    }

    fun <T> launch(vararg args: String, debug: Boolean = false, rLibsUser: Set<String> = libsUser, action: Process.() -> T): T {
        val rExe = binPath?.let {
            File(it).resolve("R.exe")
                    .absoluteFile
                    .normalize()
        } ?: throw IOException("R not found")

        return ProcessBuilder()
                .run {
                    command(rExe.absolutePath)
                    command() += args
                    if (rLibsUser.isNotEmpty()) {
                        environment() += "R_LIBS_USER" to rLibsUser.joinToString(separator = ";")
                    }
                    if (debug) {
                        redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        redirectError(ProcessBuilder.Redirect.INHERIT)
                    }
                    Logger.getLogger(R::class.java.name).info(command()
                            .joinToString(prefix = " starting -> ", separator = " ") { "\"$it\"" })
                    start()
                }
                .action()
    }
}