package io.github.paypredict.rserve

/**
 * <p>
 * Created by alexei.vylegzhanin@gmail.com on 10/29/2017.
 */
fun main(args: Array<String>) {
    RServe(debugR = true).use {
        val rexp = it eval """
            n = c(2, 3, 5)
            s = c("aa", "bb", "cc")
            b = c(TRUE, FALSE, TRUE)
            list(n, s, b)
        """
        println(rexp)
    }
}