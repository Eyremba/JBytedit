package quux.jbytedit.render

import quux.jbytedit.tree.ClassTreeNode
import java.awt.Component
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer

class CustomTreeRenderer : DefaultTreeCellRenderer() {

    override fun getTreeCellRendererComponent(tree: JTree?, value: Any?, sel: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean): Component? {
        val component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
        if (value is ClassTreeNode) {
            icon = leafIcon
        }
        return component
    }

}