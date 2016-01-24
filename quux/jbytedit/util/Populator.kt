package quux.jbytedit.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import quux.jbytedit.JBytedit
import quux.jbytedit.render.CustomTreeRenderer
import quux.jbytedit.tree.ClassTreeNode
import quux.jbytedit.tree.DirectoryTreeNode
import quux.jbytedit.tree.JavaTreeNode
import quux.jbytedit.tree.MethodTreeNode
import java.awt.Font
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.jar.JarFile
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode

object Populator {

    @Suppress("UNCHECKED_CAST") fun populateFileTree(file: JarFile) {
        val entries = Collections.list(file.entries())
        val root = DirectoryTreeNode("")
        for (entry in entries) {
            if (!entry.name.endsWith("/")){
                val parts = ArrayList<String>(entry.name.split("/"))
                val dirPath = parts.subList(0, parts.lastIndex)
                var parent = root

                while (dirPath.size > 0) {
                    var node = parent.getChild(dirPath.first())
                    if (node == null || node !is DirectoryTreeNode){
                        val newDir = DirectoryTreeNode(dirPath.first())
                        parent.add(newDir)
                        node = newDir
                    }
                    parent = node
                    dirPath.removeAt(0)
                }

                if (entry.name.endsWith(".class")){
                    val classNode = ClassNode()
                    val input = file.getInputStream(entry)
                    try {
                        val classReader = ClassReader(input)
                        classReader.accept(classNode, 0)
                    } finally {
                        input.close()
                    }
                    FileUtil.classes.put(entry.name, classNode)
                    val treeNode = ClassTreeNode(classNode)

                    for (method in (classNode.methods as List<MethodNode>))
                        treeNode.add(MethodTreeNode(method, treeNode))

                    parent.add(treeNode)
                }
                else {
                    val fileName = parts.last()
                    parent.add(DefaultMutableTreeNode(fileName))
                }
            }
        }

        root.sort()
        val tree = JTree(root)
        tree.cellRenderer = CustomTreeRenderer()
        tree.addTreeSelectionListener {
            val treeNode = it.path.lastPathComponent
            if (treeNode is MethodTreeNode){
                populateInstructionsList(treeNode.node)
            }
            else if (treeNode is ClassTreeNode){
                populateFieldsList(treeNode.node)
            }
        }
        tree.addMouseListener(object: MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (e == null){ return }
                else if (SwingUtilities.isRightMouseButton(e)){
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
                                    if (node is JavaTreeNode){
                                        node.remove(true)
                                    }
                                }
                        }
                        popup.add(remove)

                        if (tree.selectionCount == 1 && tree.selectionPath.lastPathComponent is MethodTreeNode){
                            val edit = JMenuItem("Edit")
                            edit.addActionListener {
                                Dialog.methodEditor((tree.selectionPath.lastPathComponent as MethodTreeNode).node)
                            }
                            popup.add(edit)
                        }

                        if (tree.selectionCount == 1 && tree.selectionPath.lastPathComponent is ClassTreeNode){
                            val edit = JMenuItem("Edit Access")
                            edit.addActionListener {
                                Dialog.classAccessEditor((tree.selectionPath.lastPathComponent as ClassTreeNode).node)
                            }
                            popup.add(edit)
                        }

                        popup.show(JBytedit.INSTANCE, JBytedit.INSTANCE!!.mousePosition.x, JBytedit.INSTANCE!!.mousePosition.y)
                    }
                }
            }
        })
        JBytedit.INSTANCE!!.jtree = tree
        JBytedit.INSTANCE!!.treePane.viewport.removeAll()
        JBytedit.INSTANCE!!.treePane.viewport.add(tree)
        JBytedit.INSTANCE!!.pack()
    }

    fun populateFieldsList(classNode: ClassNode){
        val displayedInstructions = Vector<String>()
        displayedInstructions.removeAllElements()
        for (field in classNode.fields){
            if (field is FieldNode){
                displayedInstructions.add(TextUtil.toHtml(OpUtil.getDisplayAccess(field.access) + " " +
                        TextUtil.addTag(OpUtil.getDisplayType(field.desc) + " " + field.name, "b") + " = " +
                        (if (field.value is String)
                            TextUtil.addTag( "\"${field.value}\"" , "font color=#559955")
                        else
                            TextUtil.addTag(field.value?.toString()?: "null" , "font color=#aa5555")) + ";"))
            }
        }
        val list = JList(displayedInstructions)
        list.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        list.addMouseListener(object: MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (e == null){ return }
                else if (SwingUtilities.isRightMouseButton(e)){
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
                                populateFieldsList(classNode)
                            }
                        }
                        popup.add(remove)
                    }
                    if (list.selectedIndices.size == 1)
                    {
                        val edit = JMenuItem("Edit")
                        edit.addActionListener {
                            Dialog.fieldEditor(classNode, classNode.fields[list.selectedIndex] as FieldNode)
                            populateFieldsList(classNode)
                        }
                        popup.add(edit)
                    }
                    val add = JMenuItem("Add")
                    add.addActionListener {
                        Dialog.fieldEditor(classNode, null)
                        populateFieldsList(classNode)
                    }
                    popup.add(add)
                    popup.show(JBytedit.INSTANCE, JBytedit.INSTANCE!!.mousePosition.x, JBytedit.INSTANCE!!.mousePosition.y)
                }
            }
        })
        JBytedit.INSTANCE!!.editorPane.viewport.removeAll()
        JBytedit.INSTANCE!!.editorPane.viewport.add(list)
    }

    fun populateInstructionsList(method: MethodNode){

        val displayedInstructions = Vector<String>()
        displayedInstructions.removeAllElements()

        OpUtil.resetLabels()
        for (insn in method.instructions) { displayedInstructions.addElement(OpUtil.getDisplayInstruction(insn as AbstractInsnNode)) }

        for (key in OpUtil.resolvedLabels.keys){
            displayedInstructions.replaceAll { it.replace(key.toString(), OpUtil.resolvedLabels[key].toString()) }
        }

        val list = JList(displayedInstructions)
        list.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        list.addMouseListener(object: MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (e == null){ return }
                else if (SwingUtilities.isRightMouseButton(e)){
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
                                if (list.selectedIndex > 0){
                                    val node = method.instructions[list.selectedIndex]
                                    method.instructions.remove(node)
                                    method.instructions.insertBefore(method.instructions[list.selectedIndex - 1], node)
                                    populateInstructionsList(method)
                                }
                            }
                            popup.add(moveUp)

                            val moveDown = JMenuItem("Move Down")
                            moveDown.addActionListener {
                                if (list.selectedIndex < method.instructions.size() - 1){
                                    val node = method.instructions[list.selectedIndex]
                                    method.instructions.remove(node)
                                    method.instructions.insert(method.instructions[list.selectedIndex], node)
                                    populateInstructionsList(method)
                                }
                            }
                            popup.add(moveDown)

                            val edit = JMenuItem("Edit")
                            edit.addActionListener {
                                Dialog.instructionEditor(method, list.selectedIndex)
                                populateInstructionsList(method)
                            }
                            popup.add(edit)
                        }
                        else {

                        }

                        val remove = JMenuItem("Remove")
                        remove.addActionListener {
                            val insnsToRemove = ArrayList<AbstractInsnNode>()
                            list.selectedIndices.forEach { insnsToRemove.add(method.instructions[it]) }
                            insnsToRemove.forEach { method.instructions.remove(it) }
                            populateInstructionsList(method)
                        }
                        popup.add(remove)
                    }

                    popup.show(JBytedit.INSTANCE, JBytedit.INSTANCE!!.mousePosition.x, JBytedit.INSTANCE!!.mousePosition.y)
                }
            }
        })
        JBytedit.INSTANCE!!.editorPane.viewport.removeAll()
        JBytedit.INSTANCE!!.editorPane.viewport.add(list)
    }
}