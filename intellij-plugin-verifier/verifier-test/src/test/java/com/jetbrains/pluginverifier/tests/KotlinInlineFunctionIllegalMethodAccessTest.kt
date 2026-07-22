package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.results.problems.IllegalMethodAccessProblem
import com.jetbrains.pluginverifier.tests.bytecode.DirectPrivateMethodAccessCallerDump
import com.jetbrains.pluginverifier.tests.bytecode.InlinedMethodAccessCallerDump
import com.jetbrains.pluginverifier.tests.bytecode.ListenerCompanionMethodDump
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reproduces the method-access counterpart of [MP-4829](https://youtrack.jetbrains.com/issue/MP-4829):
 * a `private inline fun` declared in a companion object calls a `private fun` of that same
 * companion object. Once the Kotlin compiler inlines the function's body into an instance method
 * of the outer class, the method-invocation instruction physically lives in the outer class's
 * method, so the plain caller/callee class comparison sees two different classes and reports an
 * illegal private access, even though there is no `IllegalAccessError` at runtime and the method's
 * own companion object is the true accessor. The mimicked sources are quoted in each dump's KDoc,
 * see [InlinedMethodAccessCallerDump].
 */
class KotlinInlineFunctionIllegalMethodAccessTest : BaseBytecodeTest() {

  private val pluginSpec = IdeaPluginSpec("com.example.lens", "Some Vendor")

  @Test
  fun `private method access inlined from the method's own companion object is not reported`() {
    val idePlugin = buildIdePlugin(pluginSpec) {
      dirs("com/example/plugin") {
        file("LensMarkupModelListener.class", InlinedMethodAccessCallerDump.dump())
        file("LensMarkupModelListener\$Companion.class", ListenerCompanionMethodDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertEquals(
      "A private method access inlined from a private inline fun of the method's own companion " +
        "object is not a real illegal access and must not be reported",
      emptySet<IllegalMethodAccessProblem>(),
      verificationResult.compatibilityProblems.filterIsInstance<IllegalMethodAccessProblem>().toSet()
    )
  }

  @Test
  fun `private method accessed directly by another class without any inlining is reported`() {
    val idePlugin = buildIdePlugin(pluginSpec) {
      dirs("com/example/plugin") {
        file("DirectMethodAccess.class", DirectPrivateMethodAccessCallerDump.dump())
        file("LensMarkupModelListener\$Companion.class", ListenerCompanionMethodDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertEquals(
      "Genuine cross-class private method access must still be reported",
      1,
      verificationResult.compatibilityProblems.filterIsInstance<IllegalMethodAccessProblem>().size
    )
  }

  private fun runVerification(idePlugin: IdePlugin): PluginVerificationResult.Verified {
    val ide = buildIdeWithBundledPlugins(includeKotlinStdLib = true) {}
    return VerificationRunner().runPluginVerification(ide, idePlugin) as PluginVerificationResult.Verified
  }
}
