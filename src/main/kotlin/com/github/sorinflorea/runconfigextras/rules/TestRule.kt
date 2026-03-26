package com.github.sorinflorea.runconfigextras.rules

data class TestRule(
    var selector: String = "*",
    var exclusive: Boolean = false,
    var vmArguments: String = "",
    var scriptParameters: String = "",
    var environmentVariables: String = ""
)
