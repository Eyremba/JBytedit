package quux.jbytedit.util

import org.objectweb.asm.tree.*
import quux.jbytedit.JBytedit
import quux.jbytedit.tree.JavaTreeNode
import java.util.*
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

object Edit {

    fun fixExceptionTable(node: ClassNode){
        for (method in node.methods) {
            if (method is MethodNode) {
                val iter = method.tryCatchBlocks.iterator()
                while (iter.hasNext()) {
                    val next = iter.next()
                    if (next is TryCatchBlockNode) {
                        if (!method.instructions.contains(next.start) || !method.instructions.contains(next.end) || !method.instructions.contains(next.handler) || (next.start == next.end && next.end == next.handler)) {
                            iter.remove()
                        }
                    }
                }
            }
        }
    }

    fun removeSelected(tree: JTree) {
        for (path in tree.selectionPaths) {
            val node = path.lastPathComponent as DefaultMutableTreeNode
            if (node is JavaTreeNode) {
                node.remove(true)
            }
        }
    }

    fun clearSelected(tree: JTree, selected: Boolean) {
        for (node in tree.selectionPaths) {
            val treeNode = node.lastPathComponent
            if (treeNode is JavaTreeNode) {
                treeNode.clear(selected)
            }
        }
    }

    fun removeFields(fields: MutableList<Any?>, selectedIndices: IntArray?) {
        val fieldsToRemove = ArrayList<Any?>()
        selectedIndices!!.forEach { fieldsToRemove.add(fields[it]) }
        fieldsToRemove.forEach { fields.remove(it) }
    }

    fun removeInsns(instructions: InsnList?, selectedIndices: IntArray?) {
        val insnsToRemove = ArrayList<AbstractInsnNode>()
        selectedIndices!!.forEach {
            insnsToRemove.add(instructions!![it])
            JBytedit.INSTANCE.insnListModel!!.removeElementAt(it)
        }
        insnsToRemove.forEach { instructions!!.remove(it) }
    }

    fun moveInsnBy(i: Int, instructions: InsnList, selectedIndex: Int) {
        if (selectedIndex + i > 0 && selectedIndex + i < instructions.size() - 1) {
            val node = instructions[selectedIndex + i]
            instructions.remove(node)
            JBytedit.INSTANCE.insnListModel!!.removeElementAt(selectedIndex + i)
            instructions.insert(instructions[selectedIndex - 1], node)
            var displayString = OpUtil.getDisplayInstruction(node)
            if (node is LabelNode)
                for (key in OpUtil.resolvedLabels.keys)
                    displayString.replace(key.toString(), OpUtil.resolvedLabels[key].toString())
            JBytedit.INSTANCE.insnListModel!!.add(selectedIndex, displayString)
        }
    }

    fun insertOrReplaceInsn(source: AbstractInsnNode?, target: AbstractInsnNode?, instructions: InsnList, replace: Boolean){
        JBytedit.INSTANCE.insnListModel!!.add(instructions.indexOf(target) + 1, OpUtil.getDisplayInstruction(source!!))
        instructions.insert(target, source)
        if (replace){
            JBytedit.INSTANCE.insnListModel!!.removeElementAt(instructions.indexOf(target))
            instructions.remove(target)
        }
    }

}