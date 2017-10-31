package io.github.paypredict.rserve

fun main(args: Array<String>) {
    RServe(port = 6314, debugR = true, debugRserve = false).use {
        val rexp = it eval """
            memory.limit(size=16384)
            dir_env = "D:/DATA/Renvir"
            envir_rmd <- readRDS(file.path(dir_env, "env_rmd.Rdata"))
            attach(envir_rmd)
            dic_cpt
        """.trimIndent()
        println(rexp.asNativeJavaObject())
    }
}