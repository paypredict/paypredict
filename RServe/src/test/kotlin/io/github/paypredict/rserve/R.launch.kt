package io.github.paypredict.rserve

fun main(args: Array<String>) {
    R.launch("-e", "print('123')", debug = true) {
        waitFor()
        println("exitValue: " + exitValue())
    }
}