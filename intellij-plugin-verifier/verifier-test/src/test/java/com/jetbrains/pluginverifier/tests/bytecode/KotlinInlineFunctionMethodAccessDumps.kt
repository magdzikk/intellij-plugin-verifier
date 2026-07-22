/*
 * Class-file dumps for KotlinInlineFunctionIllegalMethodAccessTest, hand-built with ASM to mimic
 * the method-access counterpart of MP-4829 (https://youtrack.jetbrains.com/issue/MP-4829): a
 * `private inline fun` declared in a companion object calls a `private fun` of that same companion
 * object. Once the compiler inlines the caller's body into an instance method of the outer class,
 * the method-invocation instruction physically lives in the outer class's method, even though the
 * source-level accessor is the companion object itself.
 */
package com.jetbrains.pluginverifier.tests.bytecode

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*

/**
 * The companion object of the plugin's own class, declaring a private method only ever meant to
 * be called from the companion's own (inline) functions:
 * ```
 * companion object {
 *   private fun computeSeverity(): Int = ...
 * }
 * ```
 */
object ListenerCompanionMethodDump {
  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(
      V11,
      ACC_PUBLIC or ACC_FINAL or ACC_SUPER,
      "com/example/plugin/LensMarkupModelListener\$Companion",
      null,
      "java/lang/Object",
      null
    )
    classWriter.visitSource("LensMarkupModelListener.kt", null)
    classWriter.visitMethod(ACC_PRIVATE or ACC_STATIC or ACC_FINAL, "computeSeverity", "()I", null, null).apply {
      visitCode()
      visitInsn(ICONST_0)
      visitInsn(IRETURN)
      visitMaxs(1, 0)
      visitEnd()
    }
    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}

/**
 * The outer plugin class:
 * ```
 * package com.example.plugin
 *
 * class LensMarkupModelListener {
 *   fun showIfValid(): Int {
 *     return runWithHighlighterIfValid()
 *   }
 *
 *   companion object {
 *     private fun computeSeverity(): Int = 0
 *     private inline fun runWithHighlighterIfValid(): Int = computeSeverity()
 *   }
 * }
 * ```
 * `showIfValid`'s compiled body contains the inlined call to the companion's own private method,
 * plus an SMAP mapping that output line back to the `Companion` class, emitted as the
 * `SourceDebugExtension` attribute.
 */
object InlinedMethodAccessCallerDump {
  private const val SMAP = "SMAP\nLensMarkupModelListener.kt\nKotlin\n*S Kotlin\n*F\n" +
    "+ 1 LensMarkupModelListener.kt\ncom/example/plugin/LensMarkupModelListener\n" +
    "+ 2 LensMarkupModelListener.kt\ncom/example/plugin/LensMarkupModelListener\$Companion\n" +
    "*L\n1#1,9:1\n64#2,3:10\n" +
    "*S KotlinDebug\n*F\n+ 1 LensMarkupModelListener.kt\ncom/example/plugin/LensMarkupModelListener\n" +
    "*L\n7#1:10,3\n*E\n"

  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(V11, ACC_PUBLIC or ACC_FINAL or ACC_SUPER, "com/example/plugin/LensMarkupModelListener", null, "java/lang/Object", null)
    classWriter.visitSource("LensMarkupModelListener.kt", SMAP)

    classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
      visitCode()
      visitVarInsn(ALOAD, 0)
      visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
      visitInsn(RETURN)
      visitMaxs(1, 1)
      visitEnd()
    }

    classWriter.visitMethod(ACC_PUBLIC or ACC_FINAL, "showIfValid", "()I", null, null).apply {
      visitCode()
      val label0 = Label()
      visitLabel(label0)
      visitLineNumber(7, label0)
      val label1 = Label()
      visitLabel(label1)
      visitLineNumber(11, label1)
      visitMethodInsn(INVOKESTATIC, "com/example/plugin/LensMarkupModelListener\$Companion", "computeSeverity", "()I", false)
      visitInsn(IRETURN)
      val label2 = Label()
      visitLabel(label2)
      visitLocalVariable("this", "Lcom/example/plugin/LensMarkupModelListener;", null, label0, label2, 0)
      visitMaxs(1, 1)
      visitEnd()
    }

    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}

/**
 * Control case: a plugin class calling another, unrelated plugin class's private method directly,
 * with no line number/SMAP suggesting any inlining. Genuine illegal private access must still be
 * reported.
 * ```
 * package com.example.plugin
 *
 * class DirectMethodAccess {
 *   fun run(): Int = OtherClassHolder.computeSeverity()
 * }
 * ```
 */
object DirectPrivateMethodAccessCallerDump {
  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(V11, ACC_PUBLIC or ACC_FINAL or ACC_SUPER, "com/example/plugin/DirectMethodAccess", null, "java/lang/Object", null)
    classWriter.visitSource("DirectMethodAccess.kt", null)

    classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
      visitCode()
      visitVarInsn(ALOAD, 0)
      visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
      visitInsn(RETURN)
      visitMaxs(1, 1)
      visitEnd()
    }

    classWriter.visitMethod(ACC_PUBLIC or ACC_FINAL, "run", "()I", null, null).apply {
      visitCode()
      val label0 = Label()
      visitLabel(label0)
      visitMethodInsn(INVOKESTATIC, "com/example/plugin/LensMarkupModelListener\$Companion", "computeSeverity", "()I", false)
      visitInsn(IRETURN)
      val label1 = Label()
      visitLabel(label1)
      visitLocalVariable("this", "Lcom/example/plugin/DirectMethodAccess;", null, label0, label1, 0)
      visitMaxs(1, 1)
      visitEnd()
    }

    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}
