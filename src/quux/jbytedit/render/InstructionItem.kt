package quux.jbytedit.render

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Frame
import quux.jbytedit.util.OpUtil

class InstructionItem(val insn: AbstractInsnNode): ListItem(){

    override fun toString(): String {
        return OpUtil.getDisplayInstruction(insn)
    }

}
