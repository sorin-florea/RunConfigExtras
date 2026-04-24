package com.github.sorinflorea.runconfigextras.actions

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunContextAction
import com.intellij.openapi.actionSystem.ActionUpdateThreadAware
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.util.IconLoader
import com.github.sorinflorea.runconfigextras.ConfigNaming
import com.github.sorinflorea.runconfigextras.settings.PluginSettings

abstract class BaseRunConfigAction : AnAction(), ActionUpdateThreadAware {

    override fun update(event: AnActionEvent) {
        val runAction = getAction(event)

        runAction?.let {
            event.presentation.icon = IconLoader.createLazy {
                runAction.executor.icon
            }
        }

        if (event.presentation.text.contains(" \'")) {
            event.presentation.text = event.presentation.text.replaceFirst(" '", " New '")
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        getAction(event)?.let {
            DelegatingRunContextAction(it).actionPerformed(event)
        }
    }

    class DelegatingRunContextAction(private val delegate: RunContextAction) : RunContextAction(delegate.executor) {
        override fun findExisting(context: ConfigurationContext): RunnerAndConfigurationSettings? {
            return null
        }

        override fun perform(configuration: RunnerAndConfigurationSettings, context: ConfigurationContext) {
            val project = context.project ?: return
            val runManager = RunManagerEx.getInstanceEx(project)
            val replaceDifferentTask = PluginSettings.instance.state.replaceExistingConfig

            val targetName = ConfigNaming.computeTargetBaseName(configuration)
            if (targetName != null) {
                ConfigNaming.resolveTargetName(targetName, runManager, replaceDifferentTask)

                val originalConfig = configuration.configuration as ExternalSystemRunConfiguration
                val clonedConfig = originalConfig.clone()
                clonedConfig.name = targetName
                val cloned = runManager.createConfiguration(clonedConfig, originalConfig.factory!!)
                cloned.isTemporary = true
                runManager.addConfiguration(cloned)
                runManager.selectedConfiguration = cloned
                ProgramRunnerUtil.executeConfiguration(cloned, delegate.executor)
            } else {
                super.perform(configuration, context)
            }
        }
    }

    protected abstract fun getAction(e: AnActionEvent): RunContextAction?
}
