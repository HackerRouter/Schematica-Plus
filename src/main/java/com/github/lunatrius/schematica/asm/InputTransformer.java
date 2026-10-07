package com.github.lunatrius.schematica.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class InputTransformer implements IClassTransformer {
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !("net.minecraft.client.Minecraft".equals(transformedName)
            || "net.minecraft.client.gui.GuiScreen".equals(transformedName))) return bytes;
        ClassNode type = new ClassNode();
        new ClassReader(bytes).accept(type, 0);
        boolean changed = false;
        for (MethodNode method : type.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!call.name.equals("next") || !call.desc.equals("()Z")) continue;
                String hook;
                if (call.owner.equals("org/lwjgl/input/Keyboard")) hook = "nextKeyboard";
                else if (call.owner.equals("org/lwjgl/input/Mouse")) hook = "nextMouse";
                else continue;
                String owner = "com/github/lunatrius/schematica/client/input/HotkeyHooks";
                AbstractInsnNode next = call.getNext();
                if (next instanceof MethodInsnNode && owner.equals(((MethodInsnNode) next).owner)
                    && hook.equals(((MethodInsnNode) next).name) && "(Z)Z".equals(((MethodInsnNode) next).desc)) continue;
                // Other input patches use the original polling calls as injection/slice anchors.
                method.instructions.insert(call, new MethodInsnNode(call.getOpcode(), owner, hook, "(Z)Z", false));
                changed = true;
            }
        }
        if (!changed) return bytes;
        ClassWriter writer = new ClassWriter(0);
        type.accept(writer);
        return writer.toByteArray();
    }
}
