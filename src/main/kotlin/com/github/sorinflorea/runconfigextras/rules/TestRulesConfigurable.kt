package com.github.sorinflorea.runconfigextras.rules

import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.DefaultCellEditor
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.table.TableCellEditor

class TestRulesConfigurable(private val project: Project) : BoundConfigurable("Test Rules") {

    private val rules = mutableListOf<TestRule>()

    private val selectorColumn = object : ColumnInfo<TestRule, String>("Selector") {
        override fun valueOf(item: TestRule) = item.selector
        override fun setValue(item: TestRule, value: String) { item.selector = value }
        override fun isCellEditable(item: TestRule) = true
        override fun getEditor(item: TestRule): TableCellEditor = DefaultCellEditor(JTextField())
        override fun getRenderer(item: TestRule) = object : com.intellij.ui.ColoredTableCellRenderer() {
            override fun customizeCellRenderer(
                table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int
            ) {
                val text = value as? String ?: ""
                append(text)
                if (text.isNotEmpty() && !TestRuleMatcher.isValidSelector(text)) {
                    foreground = com.intellij.ui.JBColor.RED
                    toolTipText = "Invalid selector. Use :subproject:task, :subproject:*, *:task, or *"
                }
            }
        }
    }

    private val vmArgsColumn = object : ColumnInfo<TestRule, String>("VM Arguments") {
        override fun valueOf(item: TestRule) = item.vmArguments
        override fun setValue(item: TestRule, value: String) { item.vmArguments = value }
        override fun isCellEditable(item: TestRule) = true
        override fun getEditor(item: TestRule): TableCellEditor = DefaultCellEditor(JTextField())
    }

    private val scriptParamsColumn = object : ColumnInfo<TestRule, String>("Script Parameters") {
        override fun valueOf(item: TestRule) = item.scriptParameters
        override fun setValue(item: TestRule, value: String) { item.scriptParameters = value }
        override fun isCellEditable(item: TestRule) = true
        override fun getEditor(item: TestRule): TableCellEditor = DefaultCellEditor(JTextField())
    }

    private val envVarsColumn = object : ColumnInfo<TestRule, String>("Env Variables") {
        override fun valueOf(item: TestRule) = item.environmentVariables
        override fun setValue(item: TestRule, value: String) { item.environmentVariables = value }
        override fun isCellEditable(item: TestRule) = true
        override fun getEditor(item: TestRule): TableCellEditor = EnvVarsCellEditor()
    }

    private val exclusiveColumn = object : ColumnInfo<TestRule, Boolean>("Exclusive") {
        override fun valueOf(item: TestRule) = item.exclusive
        override fun setValue(item: TestRule, value: Boolean) { item.exclusive = value }
        override fun isCellEditable(item: TestRule) = true
        override fun getColumnClass() = java.lang.Boolean::class.java
        override fun getWidth(table: JTable?) = 70
    }

    private val tableModel = ListTableModel(
        arrayOf(selectorColumn, vmArgsColumn, scriptParamsColumn, envVarsColumn, exclusiveColumn),
        rules
    )

    private val table = TableView(tableModel)

    override fun createPanel() = panel {
        group("Test Rules") {
            row {
                val decorator = ToolbarDecorator.createDecorator(table)
                    .setAddAction { tableModel.addRow(TestRule()); tableModel.fireTableDataChanged() }
                    .setRemoveAction {
                        val selected = table.selectedRow
                        if (selected >= 0) {
                            tableModel.removeRow(selected)
                            tableModel.fireTableDataChanged()
                        }
                    }
                cell(decorator.createPanel())
                    .align(Align.FILL)
            }.resizableRow()
            row {
                comment(
                    "Selectors: :subproject:task, :subproject:*, *:task, or * (match all)<br>" +
                    "All matching rules are merged (most specific first)<br>" +
                    "An exclusive rule stops the merge — less specific rules are ignored"
                )
            }
        }
    }

    override fun isModified(): Boolean {
        val saved = TestRulesSettings.getInstance(project).state.rules
        return rules != saved
    }

    override fun apply() {
        TestRulesSettings.getInstance(project).state.rules = rules.map { it.copy() }.toMutableList()
    }

    override fun reset() {
        rules.clear()
        rules.addAll(TestRulesSettings.getInstance(project).state.rules.map { it.copy() })
        tableModel.fireTableDataChanged()
    }

    private class EnvVarsCellEditor : AbstractCellEditor(), TableCellEditor {
        private val component = EnvironmentVariablesTextFieldWithBrowseButton()

        override fun getCellEditorValue(): Any = component.text

        override fun getTableCellEditorComponent(
            table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int
        ): Component {
            component.text = value as? String ?: ""
            return component
        }
    }
}
