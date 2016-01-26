package quux.jbytedit.forge

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import quux.jbytedit.JBytedit
import quux.jbytedit.decrypt.ZKMDecrypter
import quux.jbytedit.entry.SearchEntry
import quux.jbytedit.render.CustomTreeRenderer
import quux.jbytedit.tree.ClassTreeNode
import quux.jbytedit.tree.DirectoryTreeNode
import quux.jbytedit.tree.MethodTreeNode
import quux.jbytedit.util.FileUtil
import quux.jbytedit.util.OpUtil
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.jar.JarFile
import javax.swing.JList
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode

object Component {

    @Suppress("UNCHECKED_CAST") fun fileTree(file: JarFile): JTree {
        val entries = Collections.list(file.entries())
        val root = DirectoryTreeNode("")
        for (entry in entries) {
            if (!entry.name.endsWith("/")) {
                val parts = ArrayList<String>(entry.name.split("/"))
                val dirPath = parts.subList(0, parts.lastIndex)
                var parent = root

                while (dirPath.size > 0) {
                    var node = parent.getChild(dirPath.first())
                    if (node == null || node !is DirectoryTreeNode) {
                        val newDir = DirectoryTreeNode(dirPath.first())
                        parent.add(newDir)
                        node = newDir
                    }
                    parent = node
                    dirPath.removeAt(0)
                }

                if (entry.name.endsWith(".class")) {
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
                } else {
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
            JBytedit.INSTANCE.openNode(treeNode)
        }

        tree.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    Menu.fileTreePopup(tree)
                }
            }
        })
        return tree
    }

    fun fieldsList(classNode: ClassNode): JList<String> {

        val instructions = Vector<String>()

        for (field in classNode.fields) {
            instructions.add(OpUtil.getDisplayField(field as FieldNode))
        }

        val list = JList(instructions)
        list.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)

        list.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    Menu.fieldsPopup(classNode, list)
                }
            }
        })

        return list
    }

    fun instructionList(method: MethodNode): JList<String> {

        val instructions = Vector<String>()

        OpUtil.resetLabels()
        for (insn in method.instructions) {
            instructions.addElement(OpUtil.getDisplayInstruction(insn as AbstractInsnNode))
        }

        for (key in OpUtil.resolvedLabels.keys) {
            instructions.replaceAll { it.replace(key.toString(), OpUtil.resolvedLabels[key].toString()) }
        }

        val list = JList(instructions)
        list.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)

        list.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    Menu.instructionsPopup(method, list)
                }
            }
        })

        return list
    }

    fun zkmResult(classes : MutableCollection<ClassNode?>): JList<SearchEntry>{

        val results = Vector<SearchEntry>()

        for (classNode in classes){
            if (classNode != null) {
                var i = 0
                for (decrypted in ZKMDecrypter.decryptClass(classNode)) {
                    results.addElement(SearchEntry(classNode.name + " - $i - $decrypted", classNode, null, null))
                }
            }
        }

        if (results.size == 0){
            results.addElement(SearchEntry("No results found", null, null, null))
        }

        val list = JList(results)
        list.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)

        list.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    //Menu.instructionsPopup(method, list)
                }
            }
        })

        return list

    }
}