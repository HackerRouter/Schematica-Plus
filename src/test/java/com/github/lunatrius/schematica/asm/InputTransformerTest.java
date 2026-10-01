package com.github.lunatrius.schematica.asm;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.Assert.*;

public class InputTransformerTest {
    @Test public void hooksOnlyGameAndGuiPollingAndIsIdempotent() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "fixture/Input", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "poll", "()V", null, null);
        method.visitCode();
        for (String owner : new String[] {"org/lwjgl/input/Keyboard", "org/lwjgl/input/Mouse", "other/Input"}) {
            method.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "next", "()Z", false); method.visitInsn(Opcodes.POP);
        }
        method.visitInsn(Opcodes.RETURN); method.visitMaxs(1, 0); method.visitEnd(); writer.visitEnd();
        byte[] input = writer.toByteArray(); InputTransformer transformer = new InputTransformer();
        assertNull(transformer.transform("x", "x", null));
        assertSame(input, transformer.transform("mod/OtherScreen", "mod.OtherScreen", input));
        for (String name : new String[] {"net.minecraft.client.Minecraft", "net.minecraft.client.gui.GuiScreen"}) {
            byte[] output = transformer.transform("obfuscated", name, input);
            assertFalse(java.util.Arrays.equals(input, output));
            assertSame(output, transformer.transform("obfuscated", name, output));
            ClassNode result = new ClassNode(); new ClassReader(output).accept(result, 0);
            MethodNode poll = result.methods.get(0);
            MethodInsnNode keyboard = (MethodInsnNode) poll.instructions.get(0), mouse = (MethodInsnNode) poll.instructions.get(2);
            assertEquals("nextKeyboard", keyboard.name); assertEquals("nextMouse", mouse.name);
            assertEquals("com/github/lunatrius/schematica/client/input/HotkeyHooks", keyboard.owner);
            assertEquals(keyboard.owner, mouse.owner);
            assertEquals("other/Input", ((MethodInsnNode) poll.instructions.get(4)).owner);
        }
    }
}
