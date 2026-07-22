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
 * Reproduces [MP-4829](https://youtrack.jetbrains.com/issue/MP-4829): a `private inline fun`
 * declared in a companion object accesses a `private` field of that same companion object. Once
 * the Kotlin compiler inlines the function's body into an instance method of the outer class, the
 * field-access instruction physically lives in the outer class's method, so the plain caller/callee
 * class comparison sees two different classes and reports an illegal private access, even though
 * there is no `IllegalAccessError` at runtime and the field's own companion object is the true
 * accessor. The mimicked sources are quoted in each dump's KDoc, see [InlinedFieldAccessCallerDump].
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
