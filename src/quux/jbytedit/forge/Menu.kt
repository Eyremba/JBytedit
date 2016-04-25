package quux.jbytedit.forge

import org.objectweb.asm.tree.*
import quux.jbytedit.JBytedit
import quux.jbytedit.decrypt.ZKMDecrypter
import quux.jbytedit.entry.SearchEntry
import quux.jbytedit.render.InstructionItem
import quux.jbytedit.render.ListItem
import quux.jbytedit.tree.ClassTreeNode
import quux.jbytedit.tree.MethodTreeNode
import quux.jbytedit.util.Edit
import quux.jbytedit.util.FileUtil
import java.awt.GridLayout
import java.util.*
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
                if (!Dialog.confirm("Are you sure you want to open a new JAR file?\nAll unsaved changes will be lost")) {
                    return@addActionListener
                }
            }
            FileUtil.selectedJar = FileUtil.selectJar() ?: return@addActionListener
            try {
                JBytedit.openJar(JarFile(FileUtil.selectedJar))
            } catch (e: Exception) {
                Dialog.error("There was an error while opening the selected file!")
            }
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
        }
        fileMenu.add(saveAs)

        val tools = JMenu("Tools")
        menuBar.add(tools)

        val zkmDecrypt = JMenuItem("Decrypt ZKM Strings")
        zkmDecrypt.addActionListener {
            ZKMDecrypter.decryptClasses(FileUtil.classes.values)
        }
        tools.add(zkmDecrypt)

        val searchString = JMenuItem("Find String...")
        searchString.addActionListener {
            Dialog.searchString()
        }
        tools.add(searchString)

        return menuBar
    }

    fun instructionsPopup(method: MethodNode, parent: ClassNode, list: JList<ListItem>) {
        val popup = JPopupMenu()
        if (list.selectedIndices.size > 0) {
            if (list.selectedIndices.size == 1) {
                val insn = (list.selectedValue as InstructionItem).insn

                if (insn is MethodInsnNode){
                    val declaration = JMenuItem("Go to declaration")
                    declaration.addActionListener {
                        val owner = FileUtil.classes[insn.owner]
                        if (owner != null)
                            for (methodNode in owner.methods){
                                if (methodNode is MethodNode && methodNode.name.equals(insn.name)
                                        && methodNode.desc.equals(insn.desc))
                                    JBytedit.openMethod(methodNode, owner, list.selectedIndex)
                            }
                    }
                    popup.add(declaration)
                }

                if (insn is FieldInsnNode){
                    val declaration = JMenuItem("Go to declaration")
                    declaration.addActionListener {
                        val owner = FileUtil.classes[insn.owner]
                        if (owner != null)
                            JBytedit.openClass(owner)
                    }
                    popup.add(declaration)
                }

                if (insn is MethodInsnNode){
                    val findUsages = JMenuItem("Find usages")
                    findUsages.addActionListener {
                        val resultList = Vector<SearchEntry>()
                        for (classNode in FileUtil.classes.values)
                            for (methodNode in (classNode as ClassNode).methods){
                                var i = 0
                                for (insnNode in (methodNode as MethodNode).instructions) {
                                    if (insnNode is MethodInsnNode) {
                                        if (insnNode.owner == insn.owner && insnNode.name == insn.name &&
                                                insnNode.desc == insn.desc) {
                                            resultList.addElement(SearchEntry(classNode.name + "." + methodNode.name + methodNode.desc + " - line " + (i + 1), classNode, methodNode, insnNode))
                                        }
                                    }
                                    i++
                                }
                            }
                        JBytedit.openResults(Component.resultsList(resultList))
                    }
                    popup.add(findUsages)
                }

                if (insn is FieldInsnNode){
                    val findUsages = JMenuItem("Find usages")
                    findUsages.addActionListener {
                        val resultList = Vector<SearchEntry>()
                        for (classNode in FileUtil.classes.values)
                            for (methodNode in (classNode as ClassNode).methods){
                                var i = 0
                                for (insnNode in (methodNode as MethodNode).instructions) {
                                    if (insnNode is FieldInsnNode) {
                                        if (insnNode.owner == insn.owner && insnNode.name == insn.name &&
                                                insnNode.desc == insn.desc) {
                                            resultList.addElement(SearchEntry(classNode.name + "." + methodNode.name + methodNode.desc + " - line " + (i + 1), classNode, methodNode, insnNode))
                                        }
                                    }
                                    i++
                                }
                            }
                        JBytedit.openResults(Component.resultsList(resultList))
                    }
                    popup.add(findUsages)
                }

                val insert = JMenuItem("Insert")
                insert.addActionListener {
                    Dialog.insertInstruction(method, parent, list.selectedIndex)
                }
                popup.add(insert)

                val moveUp = JMenuItem("Move Up")
                moveUp.addActionListener {
                    Edit.moveInsnBy(-1, method.instructions, list.selectedIndex)
                }
                popup.add(moveUp)

                val moveDown = JMenuItem("Move Down")
                moveDown.addActionListener {
                    Edit.moveInsnBy(1, method.instructions, list.selectedIndex)
                }
                popup.add(moveDown)

                val edit = JMenuItem("Edit")
                edit.addActionListener {
                    Dialog.instructionEditor(method, list.selectedIndex)
                }
                popup.add(edit)
            } else {

            }

            val remove = JMenuItem("Remove")
            remove.addActionListener {
                Edit.removeInsns(method.instructions, list.selectedIndices)
                //JBytedit.INSTANCE.openMethod(method, parent, list.selectedIndex)
            }
            popup.add(remove)
        }

        popup.show(JBytedit, JBytedit.mousePosition.x, JBytedit.mousePosition.y)
    }

    fun fieldsPopup(classNode: ClassNode, list: JList<String>) {
        val popup = JPopupMenu()
        if (list.selectedIndices.size > 0) {

            val remove = JMenuItem("Remove")
            remove.addActionListener {
                if (Dialog.confirm("Are you sure you want to remove these fields?")) {
                    Edit.removeFields(classNode.fields, list.selectedIndices)
                    JBytedit.openClass(classNode)
                }
            }
            popup.add(remove)
        }
        if (list.selectedIndices.size == 1) {
            val edit = JMenuItem("Edit")
            edit.addActionListener {
                Dialog.fieldEditor(classNode, classNode.fields[list.selectedIndex] as FieldNode)
                JBytedit.openClass(classNode)
            }
            popup.add(edit)
        }
        val add = JMenuItem("Add")
        add.addActionListener {
            Dialog.fieldEditor(classNode, null)
            JBytedit.openClass(classNode)
        }
        popup.add(add)
        popup.show(JBytedit, JBytedit.mousePosition.x, JBytedit.mousePosition.y)
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

            popup.show(JBytedit, JBytedit.mousePosition.x, JBytedit.mousePosition.y)
        }
    }

    fun searchResultPopup(list: JList<SearchEntry>) {
        if (list.selectedIndices.size > 0){
            val popup = JPopupMenu()
            if (list.selectedIndices.size == 1){
                if (list.selectedValue.methodNode != null){
                    val clear = JMenuItem("Go to Method")
                    clear.addActionListener {
                        JBytedit.openMethod(list.selectedValue.methodNode!!, list.selectedValue.classNode!!,
                                list.selectedValue.methodNode!!.instructions.indexOf(list.selectedValue.insnNode))
                    }
                    popup.add(clear)
                }
                else if (list.selectedValue.classNode != null){
                    val clear = JMenuItem("Open Class")
                    clear.addActionListener {
                        val classNode = list.selectedValue.classNode
                        JBytedit.openClass(classNode)
                    }
                    popup.add(clear)
                }
            }
            var allHaveMethods = true
            list.selectedValuesList.forEach { if (it.methodNode == null) allHaveMethods = false }
            if (allHaveMethods){
                val clear = JMenuItem("Clear containing method" + (if (list.selectedIndices.size == 1) "" else "s"))
                clear.addActionListener {
                    var panel = JPanel(GridLayout(0, 1))
                    panel.add(JLabel("Are you sure you want to clear the content of these methods?"))
                    var checkbox = JCheckBox("Ignore <init> and <clinit> methods", true)
                    panel.add(checkbox)
                    val promptResult = JOptionPane.showConfirmDialog(JBytedit, panel, "Confirmation needed", JOptionPane.YES_NO_OPTION)
                    if (promptResult == JOptionPane.YES_OPTION)
                        for (value in list.selectedValuesList){
                            JBytedit.rootNode!!.getChild(value.classNode!!.name + "/" + value.methodNode!!.name)!!.clear(checkbox.isSelected)
                        }
                }
                popup.add(clear)
            }
            popup.show(JBytedit, JBytedit.mousePosition.x, JBytedit.mousePosition.y)
        }
    }

}