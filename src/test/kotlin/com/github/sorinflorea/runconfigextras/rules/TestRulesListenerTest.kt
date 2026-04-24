package com.github.sorinflorea.runconfigextras.rules

import com.intellij.execution.RunManagerEx
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@TestApplication
class TestRulesListenerTest {

    companion object {
        private val projectFixture = projectFixture()
    }

    private val project: Project get() = projectFixture.get()
    private lateinit var runManager: RunManagerEx

    @BeforeEach
    fun setUp() {
        runManager = RunManagerEx.getInstanceEx(project)
        TestRulesSettings.getInstance(project).state.rules.clear()
    }

    @AfterEach
    fun tearDown() {
        runManager.allSettings.forEach { runManager.removeConfiguration(it) }
        TestRulesSettings.getInstance(project).state.rules.clear()
    }

    private fun createGradleConfig(name: String, taskPath: String): com.intellij.execution.RunnerAndConfigurationSettings {
        val configType = GradleExternalTaskConfigurationType.getInstance()
        val factory = configType.configurationFactories[0]
        val settings = runManager.createConfiguration(name, factory)
        val config = settings.configuration as ExternalSystemRunConfiguration
        config.settings.taskNames = listOf(taskPath)
        return settings
    }

    @Test
    fun `applies vm arguments to matching temporary config`() {
        TestRulesSettings.getInstance(project).state.rules.add(
            TestRule(selector = "*:test", vmArguments = "-Dfoo=bar")
        )

        val settings = createGradleConfig("MyTest", ":core:test")
        settings.isTemporary = true
        runManager.addConfiguration(settings)

        val config = settings.configuration as ExternalSystemRunConfiguration
        assertTrue(config.settings.vmOptions?.contains("-Dfoo=bar") == true)
    }

    @Test
    fun `applies script parameters to matching temporary config`() {
        TestRulesSettings.getInstance(project).state.rules.add(
            TestRule(selector = ":core:*", scriptParameters = "--info")
        )

        val settings = createGradleConfig("MyTest", ":core:test")
        settings.isTemporary = true
        runManager.addConfiguration(settings)

        val config = settings.configuration as ExternalSystemRunConfiguration
        assertTrue(config.settings.scriptParameters?.contains("--info") == true)
    }

    @Test
    fun `applies environment variables to matching temporary config`() {
        TestRulesSettings.getInstance(project).state.rules.add(
            TestRule(selector = "*", environmentVariables = "DB_HOST=localhost")
        )

        val settings = createGradleConfig("MyTest", ":core:test")
        settings.isTemporary = true
        runManager.addConfiguration(settings)

        val config = settings.configuration as ExternalSystemRunConfiguration
        assertEquals("localhost", config.settings.env["DB_HOST"])
    }

    @Test
    fun `skips non-temporary configs`() {
        TestRulesSettings.getInstance(project).state.rules.add(
            TestRule(selector = "*", vmArguments = "-Dfoo=bar")
        )

        val settings = createGradleConfig("MyTest", ":core:test")
        settings.isTemporary = false
        runManager.addConfiguration(settings)

        val config = settings.configuration as ExternalSystemRunConfiguration
        assertNull(config.settings.vmOptions)
    }

    @Test
    fun `appends to existing vm options`() {
        TestRulesSettings.getInstance(project).state.rules.add(
            TestRule(selector = "*", vmArguments = "-Dfoo=bar")
        )

        val settings = createGradleConfig("MyTest", ":core:test")
        val config = settings.configuration as ExternalSystemRunConfiguration
        config.settings.vmOptions = "-Xmx512m"
        settings.isTemporary = true
        runManager.addConfiguration(settings)

        assertTrue(config.settings.vmOptions?.contains("-Xmx512m") == true)
        assertTrue(config.settings.vmOptions?.contains("-Dfoo=bar") == true)
    }

    @Test
    fun `merges multiple matching rules`() {
        val rules = TestRulesSettings.getInstance(project).state.rules
        rules.add(TestRule(selector = ":core:*", vmArguments = "-Da=1"))
        rules.add(TestRule(selector = "*:test", vmArguments = "-Db=2"))

        val settings = createGradleConfig("MyTest", ":core:test")
        settings.isTemporary = true
        runManager.addConfiguration(settings)

        val config = settings.configuration as ExternalSystemRunConfiguration
        val vmOpts = config.settings.vmOptions ?: ""
        assertTrue(vmOpts.contains("-Da=1"))
        assertTrue(vmOpts.contains("-Db=2"))
    }

    @Test
    fun `no matching rules leaves config unchanged`() {
        TestRulesSettings.getInstance(project).state.rules.add(
            TestRule(selector = ":api:test", vmArguments = "-Dfoo=bar")
        )

        val settings = createGradleConfig("MyTest", ":core:test")
        settings.isTemporary = true
        runManager.addConfiguration(settings)

        val config = settings.configuration as ExternalSystemRunConfiguration
        assertNull(config.settings.vmOptions)
    }
}
