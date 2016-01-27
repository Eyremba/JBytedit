package quux.jbytedit.entry

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class SearchEntry(val displayString: String, val classNode: ClassNode?, val methodNode: MethodNode?, val insnNode: AbstractInsnNode?, val index: Int) {

    override fun toString(): String {
        return displayString
    }

}