package quux.jbytedit.util

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
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
        val returnVal = fileChooser.showOpenDialog(JBytedit)
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
        var stream = FileOutputStream(outputFile.absolutePath + ".tmp")
        var output = JarOutputStream(stream)
        val file = JarFile(selectedJar)
        for (entry in file.entries()) {
            if (classes.containsKey(entry.name.slice(0..entry.name.lastIndex - 6))) {
                val writer = ClassWriter(0)
                val node = classes[entry.name.slice(0..entry.name.lastIndex - 6)] ?: continue
                output.putNextEntry(JarEntry(entry.name))
                Edit.fixExceptionTable(node)
                node.accept(writer)
                val input = writer.toByteArray()
                output.write(input)
                output.closeEntry()
            } else {
                output.putNextEntry(JarEntry(entry.name))
                val input = file.getInputStream(entry)
                val bytes = ByteArray(1024)
                while (true) {
                    val count = input.read(bytes)
                    if (count == -1) {
                        break
                    }
                    output.write(bytes, 0, count)
                }
                println(entry.name)
                output.closeEntry()
            }
        }
        output.close()
        stream.close()
        file.close()
        stream = FileOutputStream(outputFile)
        Files.copy(File(outputFile.absolutePath + ".tmp").toPath(), stream)
        stream.close()
        Files.delete(File(outputFile.absolutePath + ".tmp").toPath())
        selectedJar = outputFile

    }

}
