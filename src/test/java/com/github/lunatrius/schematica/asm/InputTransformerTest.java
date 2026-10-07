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
            MethodInsnNode originalKeyboard = (MethodInsnNode) poll.instructions.get(0), originalMouse = (MethodInsnNode) poll.instructions.get(3);
            assertEquals("org/lwjgl/input/Keyboard", originalKeyboard.owner);
            assertEquals("org/lwjgl/input/Mouse", originalMouse.owner);
            assertEquals("next", originalKeyboard.name); assertEquals("next", originalMouse.name);
            assertEquals("()Z", originalKeyboard.desc); assertEquals("()Z", originalMouse.desc);
            MethodInsnNode keyboard = (MethodInsnNode) poll.instructions.get(1), mouse = (MethodInsnNode) poll.instructions.get(4);
            assertEquals("nextKeyboard", keyboard.name); assertEquals("nextMouse", mouse.name);
            assertEquals("(Z)Z", keyboard.desc); assertEquals("(Z)Z", mouse.desc);
            assertEquals("com/github/lunatrius/schematica/client/input/HotkeyHooks", keyboard.owner);
            assertEquals(keyboard.owner, mouse.owner);
            assertEquals("other/Input", ((MethodInsnNode) poll.instructions.get(6)).owner);
        }
    }

    @Test public void keyboardPollingAnchorKeepsMouseBindingsOutsideKeyboardSlices() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "fixture/Input", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "runTick", "()V", null, null);
        method.visitCode();
        for (String device : new String[] {"Mouse", "Keyboard"}) {
            method.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/input/" + device, "next", "()Z", false);
            method.visitInsn(Opcodes.POP);
            method.visitIntInsn(Opcodes.BIPUSH, device.equals("Mouse") ? -100 : 17);
            method.visitInsn(Opcodes.ICONST_1);
            method.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/client/settings/KeyBinding", "setKeyBindState", "(IZ)V", false);
        }
        method.visitInsn(Opcodes.RETURN); method.visitMaxs(2, 0); method.visitEnd(); writer.visitEnd();
        byte[] output = new InputTransformer().transform("bao", "net.minecraft.client.Minecraft", writer.toByteArray());
        ClassNode result = new ClassNode(); new ClassReader(output).accept(result, 0);
        int anchor = -1, mouseUpdate = -1, keyboardUpdate = -1;
        org.objectweb.asm.tree.InsnList instructions = result.methods.get(0).instructions;
        for (int i = 0; i < instructions.size(); i++) {
            if (!(instructions.get(i) instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instructions.get(i);
            if (call.owner.equals("org/lwjgl/input/Keyboard") && call.name.equals("next")) anchor = i;
            if (call.name.equals("setKeyBindState")) {
                if (mouseUpdate == -1) mouseUpdate = i;
                else keyboardUpdate = i;
            }
        }
        assertTrue("Keyboard slice must start after the mouse keybinding update", mouseUpdate >= 0 && anchor > mouseUpdate);
        assertTrue(keyboardUpdate > anchor);
    }
}
