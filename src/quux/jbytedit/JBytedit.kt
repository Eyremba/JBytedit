package quux.jbytedit

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.*
import quux.jbytedit.entry.SearchEntry
import quux.jbytedit.forge.Component
import quux.jbytedit.forge.Menu
import quux.jbytedit.render.InstructionItem
import quux.jbytedit.render.ListItem
import quux.jbytedit.tree.ClassTreeNode
import quux.jbytedit.tree.DirectoryTreeNode
import quux.jbytedit.tree.MethodTreeNode
import quux.jbytedit.util.MyInterpreter
import quux.jbytedit.util.OpUtil
import quux.jbytedit.util.TextUtil
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
        val version = "v0.3.0"
    }

    lateinit var treePane: JScrollPane
    lateinit var editorPane: JScrollPane
    lateinit var searchPane: JScrollPane
    lateinit var stackAnalysisPane: JScrollPane
    lateinit var tabbedPane: JTabbedPane
    var insnListModel: DefaultListModel<ListItem>? = null
    var rootNode: DirectoryTreeNode? = null
    var fileTree: JTree? = null
    lateinit var titleLabel: JLabel
    var frames: Array<Frame> = arrayOf()

    init {
        INSTANCE = this

        UIManager.getInstalledLookAndFeels().firstOrNull { it.name.equals("Nimbus") }?.let { UIManager.setLookAndFeel(it.className) }
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
            @Suppress("UNCHECKED_CAST") override fun drop(event: DropTargetDropEvent) {
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

        treePane = JScrollPane()
        editorPane = JScrollPane()
        searchPane = JScrollPane()
        tabbedPane = JTabbedPane()
        stackAnalysisPane = JScrollPane()
        titleLabel= JLabel(" ")

        val label = JLabel("Drag and drop files to open")
        label.horizontalAlignment = JLabel.CENTER;
        label.verticalAlignment = JLabel.CENTER;
        treePane.preferredSize = Dimension(300, 500)
        add(treePane, BorderLayout.WEST)
        treePane.viewport.add(label)

        val stackPanel = JPanel(BorderLayout())
        stackPanel.preferredSize = Dimension(300, 500)
        add(stackPanel, BorderLayout.EAST)
        stackPanel.add(JLabel("Stack analysis"), BorderLayout.NORTH)
        stackPanel.add(stackAnalysisPane)

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

    var openedMethod: MethodNode? = null
    var openedClass: ClassNode? = null
    fun openMethod(method: MethodNode, parent: ClassNode, index: Int) {
        openedMethod = method
        openedClass = parent
        refreshFrames()
        titleLabel.text = "Method: " + parent.name + "." + method.name + " " + method.desc
        editorPane.viewport.removeAll()
        val openedList = Component.instructionList(method, parent)
        insnListModel = openedList.model as DefaultListModel<ListItem>
        editorPane.viewport.add(openedList)
        openedList.fixedCellHeight = 20
        openedList.ensureIndexIsVisible(index)
        openedList.selectedIndex = index
        tabbedPane.selectedIndex = 0

    }

    fun openClass(classNode: ClassNode?) {
        if (classNode == null) return
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

    fun openFrameList(index: Int) {
        stackAnalysisPane.viewport.removeAll()
        val frame = frames[index]

        fun toStr(value: BasicValue): String {
            return TextUtil.toHtml(if (value == BasicValue.UNINITIALIZED_VALUE) ""
            else if (value == BasicValue.RETURNADDRESS_VALUE) "Return Address"
            else if (value == BasicValue.REFERENCE_VALUE) "Object Reference"
            else OpUtil.getDisplayType(value.type.descriptor))
        }

        val stack = Array(frame.stackSize) {toStr(frame.getStack(it) as BasicValue)}
        val locals = Array(frame.locals) {toStr(frame.getLocal(it) as BasicValue)}
        stackAnalysisPane.viewport.add(JList(arrayOf(
                TextUtil.toHtml(TextUtil.toBold("Current stack")), *stack,
                TextUtil.toHtml(TextUtil.toBold("Current locals")), *locals)))
    }

    fun refreshFrames() {
        frames = Analyzer(MyInterpreter()).analyze(openedClass!!.name, openedMethod!!)
    }
}