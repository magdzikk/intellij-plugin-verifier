/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.isSubclassOf
import org.objectweb.asm.tree.AbstractInsnNode

fun isClassAccessibleToOtherClass(me: ClassFile, other: ClassFile): Boolean =
  me.isPublic
    || me.isPrivate && me.name == other.name
    || me.javaPackageName == other.javaPackageName
    || isKotlinDefaultConstructorMarker(me)

/**
 * In Kotlin classes the default constructor has a special parameter of type `DefaultConstructorMarker`.
 * This class is package-private but is never instantiated because `null` is always passed as its value.
 * We should not report "illegal access" for this class.
 */
private fun isKotlinDefaultConstructorMarker(classFile: ClassFile): Boolean =
  classFile.name == "kotlin/jvm/internal/DefaultConstructorMarker"

fun detectAccessProblem(
  callee: ClassFileMember,
  caller: ClassFileMember,
  context: VerificationContext,
  instructionNode: AbstractInsnNode? = null
): AccessType? {
  when {
    callee.isPrivate -> {
      if (callee is Method || callee is Field) {
        val callerClass = if (caller is ClassFile) caller else caller.containingClassFile
        val calleeClass = callee.containingClassFile
        if (doClassesBelongToTheSameNestHost(callerClass, calleeClass, context)) {
          return null
        }
        val inlineOriginClass = resolveInlineOriginClass(caller, instructionNode, context)
        return if (inlineOriginClass != null && doClassesBelongToTheSameNestHost(inlineOriginClass, calleeClass, context)) {
          // The access instruction was copied here by the Kotlin compiler when inlining a private
          // inline fun declared in calleeClass itself; the true accessor is calleeClass, not caller.
          null
        } else {
          AccessType.PRIVATE
        }
      }
      if (caller.containingClassFile.name != callee.containingClassFile.name) {
        return AccessType.PRIVATE
      }
    }
    callee.isProtected ->
      if (caller.containingClassFile.packageName != callee.containingClassFile.packageName) {
        if (!context.classResolver.isSubclassOf(caller.containingClassFile, callee.containingClassFile.name)) {
          return AccessType.PROTECTED
        }
      }
    callee.isPackagePrivate ->
      if (caller.containingClassFile.packageName != callee.containingClassFile.packageName) {
        return AccessType.PACKAGE_PRIVATE
      }
  }
  return null
}

/**
 * Resolves the class [instructionNode] was inlined from, if [caller] is a [Method] and this can
 * be established via the SMAP ([MP-4829](https://youtrack.jetbrains.com/issue/MP-4829)): a
 * `private inline fun`'s own field/method access, once inlined into another class, must still be
 * checked for accessibility against the class where it was actually declared.
 */
private fun resolveInlineOriginClass(
  caller: ClassFileMember,
  instructionNode: AbstractInsnNode?,
  context: VerificationContext
): ClassFile? {
  val callerMethod = caller as? Method ?: return null
  instructionNode ?: return null
  return KotlinInlinedCodeDetector().resolveInlineOrigin(instructionNode, callerMethod, context)
}

private fun getClassNestHost(classFile: ClassFile, context: VerificationContext): ClassFile? {
  val nestHostClassName = classFile.nestHostClass ?: return classFile
  return context.classResolver.resolveClassOrNull(nestHostClassName)
}

private fun doClassesBelongToTheSameNestHost(one: ClassFile, two: ClassFile, context: VerificationContext): Boolean {
  if (one.name == two.name) {
    return true
  }
  val oneNestHost = getClassNestHost(one, context)
  val twoNestHost = getClassNestHost(two, context)
  return oneNestHost != null && twoNestHost != null && oneNestHost.name == twoNestHost.name
}