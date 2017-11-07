package io.github.paypredict.web

class Payer(val code: String, val name: String) {
    override fun toString(): String = name
}