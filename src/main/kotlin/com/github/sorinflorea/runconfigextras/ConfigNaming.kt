package com.github.sorinflorea.runconfigextras

import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration

object ConfigNaming {

    private val NUMERIC_SUFFIX = Regex("\\s+\\(\\d+\\)$")

    /**
     * Extracts the Gradle task name from an ExternalSystemRunConfiguration.
     * Returns the short task name (after the last ':'), or null if not applicable.
     */
    fun extractTaskName(settings: RunnerAndConfigurationSettings): String? {
        val config = settings.configuration
        if (config !is ExternalSystemRunConfiguration) return null
        return config.settings.taskNames
            .firstOrNull { it.startsWith(":") }
            ?.substringAfterLast(':')
    }

    /**
     * Computes the desired name for a configuration including its Gradle task name.
     * Returns e.g. "MyTest (testJar)", or null if the config is not applicable or
     * the name already has the correct task suffix.
     */
    fun computeTargetBaseName(settings: RunnerAndConfigurationSettings): String? {
        val taskName = extractTaskName(settings) ?: return null

        val currentName = settings.name
        if (currentName.endsWith("($taskName)")) return null

        val baseName = NUMERIC_SUFFIX.find(currentName)?.let {
            currentName.substring(0, it.range.first)
        } ?: currentName

        return "$baseName ($taskName)"
    }

    /**
     * Prepares RunManager for a new config with [targetName] by removing conflicts.
     *
     * - Same test + same task (exact name match): always removes the existing temporary config.
     * - Same test + different task: only removes if [replaceDifferentTask] is true.
     * - Saved (non-temporary) configs are never removed.
     */
    fun resolveTargetName(
        targetName: String,
        runManager: RunManagerEx,
        replaceDifferentTask: Boolean
    ) {
        // Same test + same task: always replace temporary config
        runManager.allSettings
            .find { it.name == targetName && it.isTemporary }
            ?.let { runManager.removeConfiguration(it) }

        // Same test + different task: replace only if setting is enabled
        if (replaceDifferentTask) {
            val baseName = targetName.substringBeforeLast(" (")
            val baseNamePattern = Regex("^${Regex.escape(baseName)} \\(.+\\)$")
            runManager.allSettings
                .filter { it.isTemporary && it.name != targetName && baseNamePattern.matches(it.name) }
                .forEach { runManager.removeConfiguration(it) }
        }
    }
}
