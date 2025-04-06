package com.github.sorinflorea.runconfigextras.actions

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunContextAction
import com.intellij.openapi.actionSystem.ActionUpdateThreadAware
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.util.IconLoader
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

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
            val perform =
                RunContextAction::class.memberFunctions.find { it.name == "perform" && it.parameters.size == 3 }
            perform!!.isAccessible = true
            perform.call(delegate, configuration, context)
        }
    }

    protected abstract fun getAction(e: AnActionEvent): RunContextAction?
}
