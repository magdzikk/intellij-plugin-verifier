package com.jetbrains.pluginverifier.usages.util

import com.jetbrains.plugin.structure.classes.resolvers.findOriginOfType
import com.jetbrains.plugin.structure.intellij.classes.locator.PluginFileOrigin
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileMember
import com.jetbrains.pluginverifier.verifiers.resolution.KotlinInlinedCodeDetector
import com.jetbrains.pluginverifier.verifiers.resolution.Method
import org.objectweb.asm.tree.AbstractInsnNode

fun VerificationContext.isFromVerifiedPlugin(fileMember: ClassFileMember): Boolean {
  val pluginFileOrigin = fileMember.containingClassFile.classFileOrigin.findOriginOfType<PluginFileOrigin>()
  return this is PluginVerificationContext && idePlugin == pluginFileOrigin?.idePlugin
}

/**
 * Returns `true` if [instructionNode] of [callerMethod] was inlined from an `inline fun`
 * declared outside the verified plugin; `false` whenever this cannot be established reliably
 * (no line numbers, no SMAP, unresolvable origin, or the plugin's own inline function).
 */
fun KotlinInlinedCodeDetector.isInlinedFromOutsidePlugin(
  instructionNode: AbstractInsnNode,
  callerMethod: Method,
  context: VerificationContext
): Boolean {
  val originClass = resolveInlineOrigin(instructionNode, callerMethod, context) ?: return false
  return !context.isFromVerifiedPlugin(originClass)
}