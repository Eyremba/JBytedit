package quux.jbytedit.tree

import org.objectweb.asm.tree.ClassNode
import quux.jbytedit.util.FileUtil

class ClassTreeNode(val node: ClassNode) : JavaTreeNode(node.name.split("/").last()) {

    override fun remove(isTop: Boolean) {
        FileUtil.classes.put(node.name, null)
        super.remove(isTop)
    }

}
