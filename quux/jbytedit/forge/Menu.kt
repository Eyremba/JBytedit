package quux.jbytedit.forge

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import quux.jbytedit.JBytedit
import quux.jbytedit.decrypt.ZKMDecrypter
import quux.jbytedit.entry.SearchEntry
import quux.jbytedit.tree.ClassTreeNode
import quux.jbytedit.tree.MethodTreeNode
import quux.jbytedit.util.Edit
import quux.jbytedit.util.FileUtil
import java.awt.GridLayout
import java.util.jar.JarFile
import javax.swing.*

object Menu {

    fun topBar(): JMenuBar {
        val menuBar = JMenuBar()

        val fileMenu = JMenu("File")
        menuBar.add(fileMenu)

        val open = JMenuItem("Open")
        open.addActionListener {
            if (FileUtil.selectedJar != null) {
                if (Dialog.confirm("Are you sure you want to open a new JAR file?\nAll unsaved changes will be lost")) {
                    return@addActionListener
                }
            }
            FileUtil.selectedJar = FileUtil.selectJar()
            JBytedit.INSTANCE.openJar(JarFile(FileUtil.selectedJar))
        }
        fileMenu.add(open)

        val save = JMenuItem("Save")
        save.addActionListener {
            if (FileUtil.selectedJar == null)
                Dialog.error("No file open")
            else
                FileUtil.saveJar(FileUtil.selectedJar!!)
        }
        fileMenu.add(save)

        val saveAs = JMenuItem("Save As")
        saveAs.addActionListener {
            val file = FileUtil.selectJar()
            if (file != null)
                FileUtil.saveJar(file)
            else
                Dialog.error("You did not select a valid file")
        }
        fileMenu.add(saveAs)

        val tools = JMenu("Tools")
        menuBar.add(tools)

        val zkmDecrypt = JMenuItem("Decrypt ZKM Strings")
        zkmDecrypt.addActionListener {
            ZKMDecrypter.decryptClasses(FileUtil.classes.values)
        }
        tools.add(zkmDecrypt)

        return menuBar
    }

    fun instructionsPopup(method: MethodNode, list: JList<String>) {
        val popup = JPopupMenu()
        if (list.selectedIndices.size > 0) {
            if (list.selectedIndices.size == 1) {
                val insert = JMenuItem("Insert")
                insert.addActionListener {
                    Dialog.insertInstruction(method, list.selectedIndex)
                }
                popup.add(insert)

                val moveUp = JMenuItem("Move Up")
                moveUp.addActionListener {
                    Edit.moveInsnBy(-1, method.instructions, list.selectedIndex)
                    JBytedit.INSTANCE.openMethod(method)
                }
                popup.add(moveUp)

                val moveDown = JMenuItem("Move Down")
                moveDown.addActionListener {
                    Edit.moveInsnBy(1, method.instructions, list.selectedIndex)
                    JBytedit.INSTANCE.openMethod(method)
                }
                popup.add(moveDown)

                val edit = JMenuItem("Edit")
                edit.addActionListener {
                    Dialog.instructionEditor(method, list.selectedIndex)
                    JBytedit.INSTANCE.openMethod(method)
                }
                popup.add(edit)
            } else {

            }

            val remove = JMenuItem("Remove")
            remove.addActionListener {
                Edit.removeInsns(method.instructions, list.selectedIndices)
                JBytedit.INSTANCE.openMethod(method)
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
                if (Dialog.confirm("Are you sure you want to remove these fields?")) {
                    Edit.removeFields(classNode.fields, list.selectedIndices)
                    JBytedit.INSTANCE.openClass(classNode)
                }
            }
            popup.add(remove)
        }
        if (list.selectedIndices.size == 1) {
            val edit = JMenuItem("Edit")
            edit.addActionListener {
                Dialog.fieldEditor(classNode, classNode.fields[list.selectedIndex] as FieldNode)
                JBytedit.INSTANCE.openClass(classNode)
            }
            popup.add(edit)
        }
        val add = JMenuItem("Add")
        add.addActionListener {
            Dialog.fieldEditor(classNode, null)
            JBytedit.INSTANCE.openClass(classNode)
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
                    Edit.clearSelected(tree, checkbox.isSelected)
            }
            popup.add(clear)
            val remove = JMenuItem("Remove")
            remove.addActionListener {
                if (Dialog.confirm("Are you sure you want to remove these nodes?"))
                    Edit.removeSelected(tree)
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

    fun searchResultsPopup(list: JList<SearchEntry>) {
        val popupMenu = JPopupMenu()
        
        popupMenu.show(JBytedit.INSTANCE, JBytedit.INSTANCE.mousePosition.x, JBytedit.INSTANCE.mousePosition.y)
    }

}