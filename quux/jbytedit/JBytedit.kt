package quux.jbytedit

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import quux.jbytedit.entry.SearchEntry
import quux.jbytedit.forge.Component
import quux.jbytedit.forge.Menu
import quux.jbytedit.tree.ClassTreeNode
import quux.jbytedit.tree.DirectoryTreeNode
import quux.jbytedit.tree.MethodTreeNode
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.jar.JarFile
import javax.swing.*

class JBytedit : JFrame("JBytedit ${JBytedit.version}") {

    companion object {
        lateinit var INSTANCE: JBytedit
        val version = "v0.2.1"
    }

    val treePane = JScrollPane()
    val editorPane = JScrollPane()
    val searchPane = JScrollPane()
    val tabbedPane = JTabbedPane()
    var insnListModel: DefaultListModel<String>? = null
    var rootNode: DirectoryTreeNode? = null
    var fileTree: JTree? = null
    val titleLabel: JLabel = JLabel(" ")

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

        dropTarget = object : DropTarget() {
            override fun drop(event: DropTargetDropEvent) {
                try {
                    event.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                    val droppedFiles = event.transferable.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor) as List<java.io.File>
                    for (file in droppedFiles){
                       if (file.extension.toLowerCase().equals("jar")){
                           quux.jbytedit.util.FileUtil.selectedJar = file
                           openJar(java.util.jar.JarFile(java.io.File(file.absolutePath)))
                       }
                    }
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }

        jMenuBar = Menu.topBar()

        minimumSize = Dimension(900, 500)

        val label = JLabel("Drag and drop files to open")
        label.horizontalAlignment = JLabel.CENTER;
        label.verticalAlignment = JLabel.CENTER;
        treePane.preferredSize = Dimension(300, 500)
        add(treePane, BorderLayout.WEST)
        treePane.viewport.add(label)

        val panel = JPanel(BorderLayout())
        editorPane.preferredSize = Dimension(600, 500)
        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(editorPane, BorderLayout.CENTER)
        tabbedPane.addTab("Editor", panel)
        tabbedPane.addTab("Search Results", searchPane)
        tabbedPane.preferredSize = Dimension(600, 500)
        add(tabbedPane, BorderLayout.CENTER)

        //add(titleLabel, BorderLayout.NORTH)

        pack()
        isVisible = true
    }

    fun openNode(treeNode: Any?) {
        if (treeNode is MethodTreeNode) {
            openMethod(treeNode.node, treeNode.parentNode.node, 0)
        } else if (treeNode is ClassTreeNode) {
            openClass(treeNode.node)
        }
    }

    fun openJar(jar: JarFile) {
        fileTree = Component.fileTree(jar)
        rootNode = fileTree!!.model.root as DirectoryTreeNode
        treePane.viewport.removeAll()
        treePane.viewport.add(fileTree)
        jar.close()
    }

    fun openMethod(method: MethodNode, parent: ClassNode, index: Int) {
        titleLabel.text = "Method: " + parent.name + "." + method.name + " " + method.desc
        editorPane.viewport.removeAll()
        val openedList = Component.instructionList(method, parent)
        insnListModel = openedList.model as DefaultListModel<String>
        editorPane.viewport.add(openedList)
        openedList.fixedCellHeight = 20
        openedList.ensureIndexIsVisible(index)
        openedList.selectedIndex = index
        tabbedPane.selectedIndex = 0
    }

    fun openClass(classNode: ClassNode) {
        titleLabel.text = "Class: " + classNode.name
        editorPane.viewport.removeAll()
        editorPane.viewport.add(Component.fieldsList(classNode))
        tabbedPane.selectedIndex = 0
    }

    fun openResults(list: JList<SearchEntry>){
        titleLabel.text = "Search Results"
        searchPane.viewport.removeAll()
        searchPane.viewport.add(list)
        tabbedPane.selectedIndex = 1
    }
}