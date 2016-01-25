package quux.jbytedit.tree

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.MethodNode

class MethodTreeNode(val node: MethodNode, val parentNode: ClassTreeNode) : JavaTreeNode(node.name) {


    override fun clear(withInit: Boolean) {
        if (withInit && (node.name.equals("<init>") || node.name.equals("<clinit>"))) {
            return
        }

        if (parentNode.node.superName != null && (node.name.equals("<init>"))) {
            remove(true)
            return
        }

        for (insn in node.instructions) {
            if (insn is AbstractInsnNode)
                node.instructions.remove(insn)
        }

        node.exceptions.clear()

        if (node.desc.split(")")[1].startsWith("L")) {
            node.instructions.add(InsnNode(Opcodes.ACONST_NULL))
            node.instructions.add(InsnNode(Opcodes.ARETURN))
        } else if (node.desc.split(")")[1].equals("Z")) {
            node.instructions.add(InsnNode(Opcodes.ICONST_0))
            node.instructions.add(InsnNode(Opcodes.IRETURN))
        } else if (node.desc.split(")")[1].equals("B")) {
            node.instructions.add(IntInsnNode(Opcodes.BIPUSH, 0))
            node.instructions.add(InsnNode(Opcodes.IRETURN))
        } else if (node.desc.split(")")[1].equals("C")) {
            node.instructions.add(InsnNode(Opcodes.ICONST_0))
            node.instructions.add(InsnNode(Opcodes.IRETURN))
        } else if (node.desc.split(")")[1].equals("S")) {
            node.instructions.add(IntInsnNode(Opcodes.SIPUSH, 0))
            node.instructions.add(InsnNode(Opcodes.IRETURN))
        } else if (node.desc.split(")")[1].equals("I")) {
            node.instructions.add(InsnNode(Opcodes.ICONST_0))
            node.instructions.add(InsnNode(Opcodes.IRETURN))
        } else if (node.desc.split(")")[1].equals("J")) {
            node.instructions.add(InsnNode(Opcodes.LCONST_0))
            node.instructions.add(InsnNode(Opcodes.LRETURN))
        } else if (node.desc.split(")")[1].equals("F")) {
            node.instructions.add(InsnNode(Opcodes.FCONST_0))
            node.instructions.add(InsnNode(Opcodes.FRETURN))
        } else if (node.desc.split(")")[1].equals("D")) {
            node.instructions.add(InsnNode(Opcodes.DCONST_0))
            node.instructions.add(InsnNode(Opcodes.DRETURN))
        } else {
            node.instructions.add(InsnNode(Opcodes.RETURN))
        }

    }

    override fun remove(isTop: Boolean) {
        parentNode.node.methods.remove(node)
        super.remove(isTop)
    }
}