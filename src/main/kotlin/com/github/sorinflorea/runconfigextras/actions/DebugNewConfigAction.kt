package com.github.sorinflorea.runconfigextras.actions

import com.intellij.execution.actions.RunContextAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent


class DebugNewConfigAction : BaseRunConfigAction() {

    override fun getAction(e: AnActionEvent): RunContextAction? {
        return (ActionManager.getInstance().getAction("DebugClass") as RunContextAction?)
    }
}
