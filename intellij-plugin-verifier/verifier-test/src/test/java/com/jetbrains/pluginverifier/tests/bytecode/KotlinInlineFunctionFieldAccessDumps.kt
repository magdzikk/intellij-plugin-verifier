/*
 * Class-file dumps for KotlinInlineFunctionIllegalFieldAccessTest
 */
package com.jetbrains.pluginverifier.tests.bytecode

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*

/**
 * The companion object of the plugin's own class, declaring a private field only ever meant to be
 * read from the companion's own (inline) functions:
 * ```
 * companion object {
 *   private val MINIMUM_SEVERITY = ...
 * }
 * ```
 */
object ListenerCompanionDump {
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
    classWriter.visitField(ACC_PRIVATE or ACC_STATIC or ACC_FINAL, "MINIMUM_SEVERITY", "I", null, null).visitEnd()
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
 *     private val MINIMUM_SEVERITY = 0
 *     private inline fun runWithHighlighterIfValid(): Int = MINIMUM_SEVERITY
 *   }
 * }
 * ```
 * SMAP maps the inlined read (line 11) back to the `Companion` class.
 */
object InlinedFieldAccessCallerDump {
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
      visitFieldInsn(GETSTATIC, "com/example/plugin/LensMarkupModelListener\$Companion", "MINIMUM_SEVERITY", "I")
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
 * Control case: direct cross-class private field access, no SMAP. Must still be reported.
 * ```
 * package com.example.plugin
 *
 * class DirectFieldAccess {
 *   fun run(): Int = OtherClassHolder.SECRET
 * }
 * ```
 */
object DirectPrivateFieldAccessCallerDump {
  fun dump(): ByteArray {
    val classWriter = ClassWriter(0)
    classWriter.visit(V11, ACC_PUBLIC or ACC_FINAL or ACC_SUPER, "com/example/plugin/DirectFieldAccess", null, "java/lang/Object", null)
    classWriter.visitSource("DirectFieldAccess.kt", null)

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
      visitFieldInsn(GETSTATIC, "com/example/plugin/LensMarkupModelListener\$Companion", "MINIMUM_SEVERITY", "I")
      visitInsn(IRETURN)
      val label1 = Label()
      visitLabel(label1)
      visitLocalVariable("this", "Lcom/example/plugin/DirectFieldAccess;", null, label0, label1, 0)
      visitMaxs(1, 1)
      visitEnd()
    }

    classWriter.visitEnd()
    return classWriter.toByteArray()
  }
}
