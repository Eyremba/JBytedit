package quux.jbytedit.tree

import org.objectweb.asm.tree.ClassNode
import quux.jbytedit.JBytedit
import quux.jbytedit.util.FileUtil
import javax.swing.tree.DefaultMutableTreeNode

class ClassTreeNode(val node: ClassNode): JavaTreeNode(node.name.split("/").last() + ".class") {

    override fun remove(isTop: Boolean) {
        FileUtil.classes.put(node.name + ".class", null)
        super.remove(isTop)
    }

}
