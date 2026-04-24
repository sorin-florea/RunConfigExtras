package com.github.sorinflorea.runconfigextras.rules

import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project

class TestRulesListener(private val project: Project) : RunManagerListener {

    override fun runConfigurationAdded(settings: RunnerAndConfigurationSettings) {
        if (!settings.isTemporary) return

        val config = settings.configuration
        if (config !is ExternalSystemRunConfiguration) return

        val taskPath = config.settings.taskNames.firstOrNull { it.startsWith(":") } ?: return
        val lastColon = taskPath.lastIndexOf(':')
        if (lastColon <= 0) return
        val subproject = taskPath.substring(0, lastColon)
        val task = taskPath.substring(lastColon + 1)

        val rules = TestRulesSettings.getInstance(project).state.rules
        val matching = TestRuleMatcher.findMatchingRules(rules, subproject, task)
        if (matching.isEmpty()) return

        val merged = TestRuleMatcher.mergeRules(matching)

        if (merged.vmArguments.isNotBlank()) {
            val existing = config.settings.vmOptions?.takeIf { it.isNotBlank() }
            config.settings.vmOptions = listOfNotNull(existing, merged.vmArguments).joinToString(" ")
        }

        if (merged.scriptParameters.isNotBlank()) {
            val existing = config.settings.scriptParameters?.takeIf { it.isNotBlank() }
            config.settings.scriptParameters = listOfNotNull(existing, merged.scriptParameters).joinToString(" ")
        }

        if (merged.environmentVariables.isNotBlank()) {
            val env = config.settings.env.toMutableMap()
            merged.environmentVariables.split(" ").forEach { pair ->
                val eqIndex = pair.indexOf('=')
                if (eqIndex > 0) {
                    env[pair.substring(0, eqIndex)] = pair.substring(eqIndex + 1)
                }
            }
            config.settings.env = env
        }
    }
}
