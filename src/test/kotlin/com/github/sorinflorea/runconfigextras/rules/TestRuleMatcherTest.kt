package com.github.sorinflorea.runconfigextras.rules

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestRuleMatcherTest {

    // --- Step Group A: Selector validation ---

    @Test
    fun `valid selector - exact match`() {
        assertTrue(TestRuleMatcher.isValidSelector(":core:test"))
    }

    @Test
    fun `valid selector - subproject wildcard`() {
        assertTrue(TestRuleMatcher.isValidSelector(":core:*"))
    }

    @Test
    fun `valid selector - task wildcard`() {
        assertTrue(TestRuleMatcher.isValidSelector("*:test"))
    }

    @Test
    fun `valid selector - catch all`() {
        assertTrue(TestRuleMatcher.isValidSelector("*"))
    }

    @Test
    fun `invalid selector - empty string`() {
        assertFalse(TestRuleMatcher.isValidSelector(""))
    }

    @Test
    fun `invalid selector - no colon no star`() {
        assertFalse(TestRuleMatcher.isValidSelector("foo"))
    }

    @Test
    fun `invalid selector - missing task part`() {
        assertFalse(TestRuleMatcher.isValidSelector(":core:"))
    }

    @Test
    fun `valid selector - deeply nested exact match`() {
        assertTrue(TestRuleMatcher.isValidSelector(":app:core:impl:test"))
    }

    @Test
    fun `valid selector - deeply nested subproject wildcard`() {
        assertTrue(TestRuleMatcher.isValidSelector(":app:core:impl:*"))
    }

    // --- Step Group B: Matching ---

    @Test
    fun `exact selector matches exact task path`() {
        assertTrue(TestRuleMatcher.matches(":core:test", subproject = ":core", task = "test"))
    }

    @Test
    fun `exact selector does not match different task`() {
        assertFalse(TestRuleMatcher.matches(":core:test", subproject = ":core", task = "integrationTest"))
    }

    @Test
    fun `subproject wildcard matches any task in subproject`() {
        assertTrue(TestRuleMatcher.matches(":core:*", subproject = ":core", task = "integrationTest"))
    }

    @Test
    fun `subproject wildcard does not match different subproject`() {
        assertFalse(TestRuleMatcher.matches(":core:*", subproject = ":api", task = "test"))
    }

    @Test
    fun `task wildcard matches task in any subproject`() {
        assertTrue(TestRuleMatcher.matches("*:test", subproject = ":api", task = "test"))
    }

    @Test
    fun `task wildcard does not match different task`() {
        assertFalse(TestRuleMatcher.matches("*:test", subproject = ":api", task = "integrationTest"))
    }

    @Test
    fun `catch all matches anything`() {
        assertTrue(TestRuleMatcher.matches("*", subproject = ":anything", task = "whatever"))
    }

    @Test
    fun `exact selector matches deeply nested task path`() {
        assertTrue(TestRuleMatcher.matches(":app:core:impl:test", subproject = ":app:core:impl", task = "test"))
    }

    @Test
    fun `subproject wildcard matches deeply nested subproject`() {
        assertTrue(TestRuleMatcher.matches(":app:core:impl:*", subproject = ":app:core:impl", task = "integrationTest"))
    }

    @Test
    fun `task wildcard matches deeply nested subproject`() {
        assertTrue(TestRuleMatcher.matches("*:test", subproject = ":app:core:impl", task = "test"))
    }

    // --- Step Group C: Specificity and merging ---

    @Test
    fun `specificity ordering - exact is most specific`() {
        val rules = listOf(
            TestRule(selector = "*", vmArguments = "-Da=1"),
            TestRule(selector = ":core:test", vmArguments = "-Dc=3"),
            TestRule(selector = "*:test", vmArguments = "-Db=2"),
        )
        val sorted = TestRuleMatcher.findMatchingRules(rules, subproject = ":core", task = "test")
        assertEquals(listOf("-Dc=3", "-Db=2", "-Da=1"), sorted.map { it.vmArguments })
    }

    @Test
    fun `merge concatenates all fields`() {
        val rules = listOf(
            TestRule(selector = ":core:*", vmArguments = "-Da=1", scriptParameters = "--info"),
            TestRule(selector = "*:test", environmentVariables = "DB=local"),
        )
        val merged = TestRuleMatcher.mergeRules(
            TestRuleMatcher.findMatchingRules(rules, subproject = ":core", task = "test")
        )
        assertEquals("-Da=1", merged.vmArguments)
        assertEquals("--info", merged.scriptParameters)
        assertEquals("DB=local", merged.environmentVariables)
    }

    @Test
    fun `exclusive rule drops less specific rules`() {
        val rules = listOf(
            TestRule(selector = ":core:test", vmArguments = "-Da=1", exclusive = true),
            TestRule(selector = "*:test", vmArguments = "-Db=2"),
            TestRule(selector = "*", vmArguments = "-Dc=3"),
        )
        val merged = TestRuleMatcher.mergeRules(
            TestRuleMatcher.findMatchingRules(rules, subproject = ":core", task = "test")
        )
        assertEquals("-Da=1", merged.vmArguments)
    }

    @Test
    fun `exclusive at mid specificity keeps higher specificity`() {
        val rules = listOf(
            TestRule(selector = ":core:test", vmArguments = "-Da=1"),
            TestRule(selector = "*:test", vmArguments = "-Db=2", exclusive = true),
            TestRule(selector = "*", vmArguments = "-Dc=3"),
        )
        val merged = TestRuleMatcher.mergeRules(
            TestRuleMatcher.findMatchingRules(rules, subproject = ":core", task = "test")
        )
        assertEquals("-Da=1 -Db=2", merged.vmArguments)
    }

    @Test
    fun `no matching rules returns empty merge result`() {
        val rules = listOf(
            TestRule(selector = ":api:test", vmArguments = "-Da=1"),
        )
        val merged = TestRuleMatcher.mergeRules(
            TestRuleMatcher.findMatchingRules(rules, subproject = ":core", task = "test")
        )
        assertEquals("", merged.vmArguments)
        assertEquals("", merged.scriptParameters)
        assertEquals("", merged.environmentVariables)
    }
}
