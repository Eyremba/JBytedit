package quux.jbytedit

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import quux.jbytedit.forge.Component
import quux.jbytedit.forge.Menu
import quux.jbytedit.tree.ClassTreeNode
import quux.jbytedit.tree.MethodTreeNode
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.jar.JarFile
import javax.swing.*

class JBytedit : JFrame("JBytedit ${JBytedit.version}") {

    companion object {
        lateinit var INSTANCE: JBytedit
        val version = "v0.1"
    }

    val treePane = JScrollPane()
    val editorPane = JScrollPane()
    var fileTree: JTree? = null

    init {
        INSTANCE = this

        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                val promptResult = JOptionPane.showConfirmDialog(quux.jbytedit.JBytedit.INSTANCE,
                        "Are you sure you want to exit?\nAll unsaved changes will be lost",
                        "Confirmation needed", JOptionPane.YES_NO_OPTION)
                if (promptResult == JOptionPane.YES_OPTION) {
                    System.exit(0)
                }
            }
        })

        jMenuBar = Menu.topBar()

        minimumSize = Dimension(900, 500)

        treePane.preferredSize = Dimension(300, 500)
        add(treePane, BorderLayout.WEST)

        editorPane.preferredSize = Dimension(600, 500)
        add(editorPane, BorderLayout.CENTER)

        pack()
        isVisible = true
    }

    fun openNode(treeNode: Any?) {
        if (treeNode is MethodTreeNode) {
            openMethod(treeNode.node)
        } else if (treeNode is ClassTreeNode) {
            openClass(treeNode.node)
        }
    }

    fun openJar(jar: JarFile) {
        fileTree = Component.fileTree(jar)
        treePane.viewport.removeAll()
        treePane.viewport.add(fileTree)
    }

    fun openMethod(method: MethodNode) {
        editorPane.viewport.removeAll()
        editorPane.viewport.add(Component.instructionList(method))
    }

    fun openClass(classNode: ClassNode) {
        editorPane.viewport.removeAll()
        editorPane.viewport.add(Component.fieldsList(classNode))
    }
}