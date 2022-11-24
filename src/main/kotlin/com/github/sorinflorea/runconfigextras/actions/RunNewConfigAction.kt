package com.github.sorinflorea.runconfigextras.actions

import com.intellij.execution.actions.RunContextAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent


class RunNewConfigAction : BaseRunConfigAction() {

    override fun getAction(e: AnActionEvent): RunContextAction? {
        return (ActionManager.getInstance().getAction("RunClass") as RunContextAction?)
    }
}
