package quux.jbytedit.util

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode
import quux.jbytedit.JBytedit
import quux.jbytedit.forge.Dialog
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

object FileUtil {

    var selectedJar: File? = null
    val classes = HashMap<String, ClassNode?>()

    fun selectJar(): File? {
        val fileChooser = JFileChooser()
        val filter = FileNameExtensionFilter("Jar files", "jar")
        fileChooser.fileFilter = filter
        val returnVal = fileChooser.showOpenDialog(JBytedit.INSTANCE)
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (fileChooser.selectedFile.extension.toLowerCase().equals("jar")) {
                return fileChooser.selectedFile
            } else {
                Dialog.error("You did not select a valid file")
            }
        }
        return null
    }

    fun saveJar(outputFile: File) {
        var output = JarOutputStream(FileOutputStream(outputFile.absolutePath + ".tmp"))
        val file = JarFile(selectedJar)
        for (entry in file.entries()) {
            if (classes.containsKey(entry.name)) {
                val writer = ClassWriter(0)
                val node = classes[entry.name] ?: continue
                output.putNextEntry(JarEntry(entry.name))
                for (method in node.methods) {
                    if (method is MethodNode) {
                        val iter = method.tryCatchBlocks.iterator()
                        while (iter.hasNext()) {
                            val next = iter.next()
                            if (next is TryCatchBlockNode) {
                                if (!method.instructions.contains(next.start) || !method.instructions.contains(next.end) || !method.instructions.contains(next.handler) || (next.start == next.end && next.end == next.handler)) {
                                    iter.remove()
                                    println(entry.name)
                                }
                            }
                        }
                    }
                }
                node.accept(writer)
                val input = writer.toByteArray()
                output.write(input)
                output.closeEntry()
            } else {
                output.putNextEntry(entry)
                val input = file.getInputStream(entry)
                val bytes = ByteArray(1024)
                while (true) {
                    val count = input.read(bytes)
                    if (count == -1) {
                        break
                    }
                    output.write(bytes, 0, count)
                }
                output.closeEntry()
            }
        }
        output.close()
        Files.copy(File(outputFile.absolutePath + ".tmp").toPath(), FileOutputStream(outputFile))
        Files.delete(File(outputFile.absolutePath + ".tmp").toPath())
        selectedJar = outputFile

    }

}
