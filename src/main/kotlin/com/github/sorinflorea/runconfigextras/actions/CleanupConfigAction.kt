package com.github.sorinflorea.runconfigextras.actions

import com.intellij.execution.RunManagerEx
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class CleanupConfigAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val runManager = RunManagerEx.getInstanceEx(event.project ?: throw NoSuchElementException("Project is missing"))

        runManager.tempConfigurationsList.forEach {
            runManager.removeConfiguration(it)
        }

        if (runManager.selectedConfiguration == null) {
            runManager.selectedConfiguration = runManager.allSettings.firstOrNull()
        }
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }
}
