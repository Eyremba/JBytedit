package quux.jbytedit

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.*
import quux.jbytedit.render.CustomTreeRenderer
import quux.jbytedit.tree.*
import quux.jbytedit.util.*
import quux.jbytedit.util.Dialog
import java.awt.*
import java.awt.event.*
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.tree.DefaultMutableTreeNode

class JBytedit: JFrame("JBytedit") {

    companion object {
        var INSTANCE: JBytedit? = null
        val version = "v0.1"
    }

    val treePane: JScrollPane
    var jtree: JTree? = null
    val editorPane: JScrollPane

    init {
        title = "JBytedit $version"
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter(){
            override fun windowClosing(e: WindowEvent?) {
                val promptResult = JOptionPane.showConfirmDialog(quux.jbytedit.JBytedit.INSTANCE,
                        "Are you sure you want to exit?\nAll unsaved changes will be lost",
                        "Confirmation needed", JOptionPane.YES_NO_OPTION)
                if (promptResult == JOptionPane.YES_OPTION){
                    System.exit(0)
                }
            }
        })
        minimumSize = Dimension(900, 500)

        addMenu()

        treePane = JScrollPane()
        treePane.preferredSize = Dimension(300, 500)
        add(treePane, BorderLayout.WEST)

        editorPane = JScrollPane()
        editorPane.preferredSize = Dimension(600, 500)
        add(editorPane, BorderLayout.CENTER)

        pack()
        isVisible = true
    }

    private fun addMenu(){
        val menuBar = JMenuBar()
        val menu = JMenu("File")
        menuBar.add(menu)
        val open = JMenuItem("Open")
        open.addActionListener {
            if (FileUtil.selectedJar != null) {
                val promptResult = JOptionPane.showConfirmDialog(quux.jbytedit.JBytedit.INSTANCE,
                        "Are you sure you want to open a new JAR file?\nAll unsaved changes will be lost",
                        "Confirmation needed", JOptionPane.YES_NO_OPTION)
                if (promptResult == JOptionPane.YES_OPTION) {
                    FileUtil.selectedJar = FileUtil.selectJar()
                    Populator.populateFileTree(JarFile(FileUtil.selectedJar))
                }
            }
            else {
                FileUtil.selectedJar = FileUtil.selectJar()
                Populator.populateFileTree(JarFile(FileUtil.selectedJar))
            }
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

        jMenuBar = menuBar
    }
}