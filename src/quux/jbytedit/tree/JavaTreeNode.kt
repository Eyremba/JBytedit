package quux.jbytedit.tree

import quux.jbytedit.JBytedit
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

open class JavaTreeNode(name: String) : DefaultMutableTreeNode(name) {

    open fun clear(withInit: Boolean) {
        children?.forEach { if (it is JavaTreeNode) it.clear(withInit) }
    }

    open fun remove(isTop: Boolean) {
        val iter = children?.iterator()
        if (iter != null) {
            while (iter.hasNext()) {
                val child = iter.next()
                if (child is ClassTreeNode) {
                    iter.remove()
                    child.remove(false)
                } else if (child is DirectoryTreeNode) {
                    iter.remove()
                    child.remove(false)
                }
            }
        }
        if (isTop)
            (JBytedit.fileTree!!.model as DefaultTreeModel).removeNodeFromParent(this)
    }

    fun getChild(name: String): JavaTreeNode? {
        val path = name.split("/")
        for (child in children()) {
            if (child is JavaTreeNode) {
                if (child.toString().equals(path.first())) {
                    if (path.size == 1)
                        return child
                    return child.getChild(path.slice(1..path.lastIndex).joinToString("/"))
                }
            }
        }
        return null
    }

}
