package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.results.problems.IllegalFieldAccessProblem
import com.jetbrains.pluginverifier.tests.bytecode.DirectPrivateFieldAccessCallerDump
import com.jetbrains.pluginverifier.tests.bytecode.InlinedFieldAccessCallerDump
import com.jetbrains.pluginverifier.tests.bytecode.ListenerCompanionDump
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reproduces https://youtrack.jetbrains.com/issue/MP-4829
 */
class KotlinInlineFunctionIllegalFieldAccessTest : BaseBytecodeTest() {

  private val pluginSpec = IdeaPluginSpec("com.example.lens", "Some Vendor")

  @Test
  fun `private field access inlined from the field's own companion object is not reported`() {
    val idePlugin = buildIdePlugin(pluginSpec) {
      dirs("com/example/plugin") {
        file("LensMarkupModelListener.class", InlinedFieldAccessCallerDump.dump())
        file("LensMarkupModelListener\$Companion.class", ListenerCompanionDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertEquals(
      "A private field access inlined from a private inline fun of the field's own companion " +
        "object is not a real illegal access and must not be reported",
      emptySet<IllegalFieldAccessProblem>(),
      verificationResult.compatibilityProblems.filterIsInstance<IllegalFieldAccessProblem>().toSet()
    )
  }

  @Test
  fun `private field accessed directly by another class without any inlining is reported`() {
    val idePlugin = buildIdePlugin(pluginSpec) {
      dirs("com/example/plugin") {
        file("DirectFieldAccess.class", DirectPrivateFieldAccessCallerDump.dump())
        file("LensMarkupModelListener\$Companion.class", ListenerCompanionDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertEquals(
      "Genuine cross-class private field access must still be reported",
      1,
      verificationResult.compatibilityProblems.filterIsInstance<IllegalFieldAccessProblem>().size
    )
  }

  private fun runVerification(idePlugin: IdePlugin): PluginVerificationResult.Verified {
    val ide = buildIdeWithBundledPlugins(includeKotlinStdLib = true) {}
    return VerificationRunner().runPluginVerification(ide, idePlugin) as PluginVerificationResult.Verified
  }
}
