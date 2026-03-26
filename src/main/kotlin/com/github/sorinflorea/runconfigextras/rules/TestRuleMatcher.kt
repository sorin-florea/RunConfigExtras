package com.github.sorinflorea.runconfigextras.rules

object TestRuleMatcher {

    private val EXACT_PATTERN = Regex("^:.+:.+$")        // :sub:task (any depth)
    private val SUB_WILDCARD = Regex("^:.+:\\*$")         // :sub:*
    private val TASK_WILDCARD = Regex("^\\*:.+$")         // *:task
    private val CATCH_ALL = Regex("^\\*$")                // *

    fun isValidSelector(selector: String): Boolean {
        return EXACT_PATTERN.matches(selector)
            || SUB_WILDCARD.matches(selector)
            || TASK_WILDCARD.matches(selector)
            || CATCH_ALL.matches(selector)
    }

    fun matches(selector: String, subproject: String, task: String): Boolean {
        return when {
            CATCH_ALL.matches(selector) -> true
            TASK_WILDCARD.matches(selector) -> {
                val selectorTask = selector.substringAfter("*:")
                selectorTask == task
            }
            SUB_WILDCARD.matches(selector) -> {
                val selectorSub = selector.substringBeforeLast(":*")
                selectorSub == subproject
            }
            EXACT_PATTERN.matches(selector) -> {
                val selectorSub = selector.substringBeforeLast(":")
                val selectorTask = selector.substringAfterLast(":")
                selectorSub == subproject && selectorTask == task
            }
            else -> false
        }
    }

    data class MergedResult(
        val vmArguments: String,
        val scriptParameters: String,
        val environmentVariables: String
    )

    private fun specificity(selector: String): Int = when {
        EXACT_PATTERN.matches(selector) -> 3
        SUB_WILDCARD.matches(selector) -> 2
        TASK_WILDCARD.matches(selector) -> 1
        CATCH_ALL.matches(selector) -> 0
        else -> -1
    }

    fun findMatchingRules(rules: List<TestRule>, subproject: String, task: String): List<TestRule> {
        return rules
            .filter { matches(it.selector, subproject, task) }
            .sortedByDescending { specificity(it.selector) }
    }

    fun mergeRules(sortedRules: List<TestRule>): MergedResult {
        if (sortedRules.isEmpty()) return MergedResult("", "", "")

        val effective = mutableListOf<TestRule>()
        for (rule in sortedRules) {
            effective.add(rule)
            if (rule.exclusive) break
        }

        return MergedResult(
            vmArguments = effective.mapNotNull { it.vmArguments.ifBlank { null } }.joinToString(" "),
            scriptParameters = effective.mapNotNull { it.scriptParameters.ifBlank { null } }.joinToString(" "),
            environmentVariables = effective.mapNotNull { it.environmentVariables.ifBlank { null } }.joinToString(" ")
        )
    }
}
