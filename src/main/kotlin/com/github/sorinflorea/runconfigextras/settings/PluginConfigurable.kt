package com.github.sorinflorea.runconfigextras.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

class PluginConfigurable : BoundConfigurable("Run Configuration Extras") {

    override fun createPanel() = panel {
        group("Gradle Task Name in Configuration Name") {
            row {
                checkBox("Replace configuration for the same test when run with a different task")
                    .bindSelected(PluginSettings.instance.state::replaceExistingConfig)
            }
        }
    }
}
