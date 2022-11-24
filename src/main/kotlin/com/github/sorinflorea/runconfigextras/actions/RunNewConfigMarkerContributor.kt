package com.github.sorinflorea.runconfigextras.actions

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.psi.PsiElement


class RunNewConfigMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info {
        val runAction = ActionManager.getInstance().getAction("RunConfigExtras.RunNewConfigurationAction")
        val debugAction = ActionManager.getInstance().getAction("RunConfigExtras.DebugNewConfigurationAction")
        return Info(debugAction.templatePresentation.icon, null, runAction, debugAction)
    }
}
