package quux.jbytedit.tree

import java.util.*
import javax.swing.tree.DefaultMutableTreeNode

class DirectoryTreeNode(name: String) : JavaTreeNode(name) {

    fun sort() {
        Collections.sort(children, { e1, e2 ->
            (if (e1 is DirectoryTreeNode)
                (if (e2 is DirectoryTreeNode)
                    e1.toString().compareTo(e2.toString())
                else -1)
            else
                (if (e2 is DirectoryTreeNode)
                    1
                else e1.toString().compareTo(e2.toString())))
        })

        for (child in children) {
            if (child is DirectoryTreeNode) {
                child.sort()
            }
        }
    }

    fun getChild(name: String): DefaultMutableTreeNode? {
        for (child in children()) {
            if (child is DefaultMutableTreeNode) {
                if (child.toString().equals(name)) {
                    return child
                }
            }
        }
        return null
    }

}