package com.github.sorinflorea.runconfigextras.rules

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@State(name = "RunConfigExtrasTestRules", storages = [Storage("runConfigExtrasTestRules.xml")])
@Service(Service.Level.PROJECT)
class TestRulesSettings : PersistentStateComponent<TestRulesSettings.State> {

    class State {
        var rules: MutableList<TestRule> = mutableListOf()
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(project: Project): TestRulesSettings {
            return project.getService(TestRulesSettings::class.java)
        }
    }
}
