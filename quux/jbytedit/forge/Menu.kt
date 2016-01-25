package quux.jbytedit.forge

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import quux.jbytedit.JBytedit
import quux.jbytedit.tree.ClassTreeNode
import quux.jbytedit.tree.JavaTreeNode
import quux.jbytedit.tree.MethodTreeNode
import quux.jbytedit.util.FileUtil
import java.awt.GridLayout
import java.util.*
import java.util.jar.JarFile
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode

object Menu {

    fun topBar(): JMenuBar {
        val menuBar = JMenuBar()

        val menu = JMenu("File")
        menuBar.add(menu)

        val open = JMenuItem("Open")
        open.addActionListener {
            if (FileUtil.selectedJar != null) {
                val promptResult = JOptionPane.showConfirmDialog(quux.jbytedit.JBytedit.INSTANCE,
                        "Are you sure you want to open a new JAR file?\nAll unsaved changes will be lost",
                        "Confirmation needed", JOptionPane.YES_NO_OPTION)
                if (promptResult == JOptionPane.NO_OPTION) {
                    return@addActionListener
                }
            }
            FileUtil.selectedJar = FileUtil.selectJar()
            JBytedit.INSTANCE.openJar(JarFile(FileUtil.selectedJar))
        }
        menu.add(open)

        val save = JMenuItem("Save")
        save.addActionListener {
            if (FileUtil.selectedJar == null)
                Dialog.error("No file open")
            else
                FileUtil.saveJar(FileUtil.selectedJar!!)
        }
        menu.add(save)

        val saveAs = JMenuItem("Save As")
        saveAs.addActionListener {
            val file = FileUtil.selectJar()
            if (file != null)
                FileUtil.saveJar(file)
            else
                Dialog.error("You did not select a valid file")
        }
        menu.add(saveAs)

        return menuBar
    }

    fun instructionsPopup(method: MethodNode, list: JList<String>) {
        val popup = JPopupMenu()
        if (list.selectedIndices.size > 0) {
            if (list.selectedIndices.size == 1) {
                val insert = JMenuItem("Insert")
                insert.addActionListener {
                    Dialog.insructionInserter(method, list.selectedIndex)
                }
                popup.add(insert)

                val moveUp = JMenuItem("Move Up")
                moveUp.addActionListener {
                    if (list.selectedIndex > 0) {
                        val node = method.instructions[list.selectedIndex]
                        method.instructions.remove(node)
                        method.instructions.insertBefore(method.instructions[list.selectedIndex - 1], node)
                        Component.instructionList(method)
                    }
                }
                popup.add(moveUp)

                val moveDown = JMenuItem("Move Down")
                moveDown.addActionListener {
                    if (list.selectedIndex < method.instructions.size() - 1) {
                        val node = method.instructions[list.selectedIndex]
                        method.instructions.remove(node)
                        method.instructions.insert(method.instructions[list.selectedIndex], node)
                        Component.instructionList(method)
                    }
                }
                popup.add(moveDown)

                val edit = JMenuItem("Edit")
                edit.addActionListener {
                    Dialog.instructionEditor(method, list.selectedIndex)
                    Component.instructionList(method)
                }
                popup.add(edit)
            } else {

            }

            val remove = JMenuItem("Remove")
            remove.addActionListener {
                val insnsToRemove = ArrayList<AbstractInsnNode>()
                list.selectedIndices.forEach { insnsToRemove.add(method.instructions[it]) }
                insnsToRemove.forEach { method.instructions.remove(it) }
                Component.instructionList(method)
            }
            popup.add(remove)
        }

        popup.show(JBytedit.INSTANCE, JBytedit.INSTANCE.mousePosition.x, JBytedit.INSTANCE.mousePosition.y)
    }

    fun fieldsPopup(classNode: ClassNode, list: JList<String>) {
        val popup = JPopupMenu()
        if (list.selectedIndices.size > 0) {

            val remove = JMenuItem("Remove")
            remove.addActionListener {
                var panel = JPanel(GridLayout(0, 1))
                panel.add(JLabel("Are you sure you want to remove these fields?"))
                val promptResult = JOptionPane.showConfirmDialog(list, panel, "Confirmation needed", JOptionPane.YES_NO_OPTION)
                if (promptResult == JOptionPane.YES_OPTION) {
                    val fieldsToRemove = ArrayList<Any?>()
                    list.selectedIndices.forEach { fieldsToRemove.add(classNode.fields[it]) }
                    fieldsToRemove.forEach { classNode.fields.remove(it) }
                    Component.fieldsList(classNode)
                }
            }
            popup.add(remove)
        }
        if (list.selectedIndices.size == 1) {
            val edit = JMenuItem("Edit")
            edit.addActionListener {
                Dialog.fieldEditor(classNode, classNode.fields[list.selectedIndex] as FieldNode)
                Component.fieldsList(classNode)
            }
            popup.add(edit)
        }
        val add = JMenuItem("Add")
        add.addActionListener {
            Dialog.fieldEditor(classNode, null)
            Component.fieldsList(classNode)
        }
        popup.add(add)
        popup.show(JBytedit.INSTANCE, JBytedit.INSTANCE.mousePosition.x, JBytedit.INSTANCE.mousePosition.y)
    }

    fun fileTreePopup(tree: JTree) {
        if (tree.selectionCount > 0) {
            val popup = JPopupMenu()

            val clear = JMenuItem("Clear")
            clear.addActionListener {
                var panel = JPanel(GridLayout(0, 1))
                panel.add(JLabel("Are you sure you want to clear the content of these nodes?"))
                var checkbox = JCheckBox("Ignore <init> and <clinit> methods", true)
                panel.add(checkbox)
                val promptResult = JOptionPane.showConfirmDialog(tree, panel, "Confirmation needed", JOptionPane.YES_NO_OPTION)
                if (promptResult == JOptionPane.YES_OPTION)
                    for (node in tree.selectionPaths) {
                        val treeNode = node.lastPathComponent
                        if (treeNode is JavaTreeNode) {
                            treeNode.clear(checkbox.isSelected)
                        }
                    }
            }
            popup.add(clear)
            val remove = JMenuItem("Remove")
            remove.addActionListener {
                var panel = JPanel(GridLayout(0, 1))
                panel.add(JLabel("Are you sure you want to remove these nodes?"))
                val promptResult = JOptionPane.showConfirmDialog(tree, panel, "Confirmation needed", JOptionPane.YES_NO_OPTION)
                if (promptResult == JOptionPane.YES_OPTION)
                    for (path in tree.selectionPaths) {
                        val node = path.lastPathComponent as DefaultMutableTreeNode
                        if (node is JavaTreeNode) {
                            node.remove(true)
                        }
                    }
            }
            popup.add(remove)

            if (tree.selectionCount == 1 && tree.selectionPath.lastPathComponent is MethodTreeNode) {
                val edit = JMenuItem("Edit")
                edit.addActionListener {
                    Dialog.methodEditor((tree.selectionPath.lastPathComponent as MethodTreeNode).node)
                }
                popup.add(edit)
            }

            if (tree.selectionCount == 1 && tree.selectionPath.lastPathComponent is ClassTreeNode) {
                val edit = JMenuItem("Edit Access")
                edit.addActionListener {
                    Dialog.classAccessEditor((tree.selectionPath.lastPathComponent as ClassTreeNode).node)
                }
                popup.add(edit)
            }

            popup.show(JBytedit.INSTANCE, JBytedit.INSTANCE.mousePosition.x, JBytedit.INSTANCE.mousePosition.y)
        }
    }

}