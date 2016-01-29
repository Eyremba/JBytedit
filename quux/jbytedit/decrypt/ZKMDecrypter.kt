package quux.jbytedit.decrypt

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import quux.jbytedit.util.OpUtil
import java.util.*

object ZKMDecrypter {

    fun decryptClasses(classes : MutableCollection<ClassNode?>){
        for (classNode in classes){
            if (classNode != null){
                decryptClass(classNode)
            }
        }
    }

    @Suppress("UNCHECKED_CAST") private fun decryptClass(classNode: ClassNode) {

        var clinit: MethodNode? = null
        for (method in classNode.methods){
            if (method is MethodNode){
                if (method.name.equals("<clinit>")){
                    clinit = method
                    break
                }
            }
        }

        if (clinit == null){
            return
        }

        var modifiers = getModifiers(clinit)
        if (modifiers.size == 0) {
            return specialDecryptClass(classNode)
        }

        val encList = ArrayList<String>()
        val decList = ArrayList<String>()
        var stringHolder = ""

        for (insn in clinit.instructions) {
            if (insn is AbstractInsnNode) {
                if (insn is LdcInsnNode && insn.cst is String) {
                    encList.add(insn.cst as String)
                }
                if (stringHolder.equals("") && insn is FieldInsnNode && insn.opcode == Opcodes.PUTSTATIC && insn.owner.equals(classNode.name) && insn.desc.contains("Ljava/lang/String;")){
                   stringHolder = insn.desc + insn.owner + insn.name
                }
            }
        }

        encList.forEach { decList.add(decryptString(it, modifiers)) }

        var match = false
        for (method in classNode.methods){
            var indices = ArrayList<Int>()
            var strings = ArrayList<String>()
            var i = 0
            val iter = (method as MethodNode).instructions.iterator()
            while (iter.hasNext()){
                val insn = iter.next() as AbstractInsnNode
                if (!match) {
                    if (insn.opcode == Opcodes.GETSTATIC && insn is FieldInsnNode &&
                            stringHolder.equals(insn.desc + insn.owner + insn.name)) {
                        match = true
                        iter.remove()
                    }
                    else {
                        i++
                    }
                }
                else {
                    if (stringHolder.startsWith("[")) {
                        indices.add(i)
                        strings.add(decList[OpUtil.getIntValue(insn)])
                        iter.remove()
                        iter.next()
                        iter.remove()
                    }
                    else {
                        indices.add(i)
                        strings.add(decList[0])
                    }
                    match = false
                }
            }
            indices.reverse()
            strings.reverse()
            for (index in 0..indices.size - 1){
                method.instructions.insertBefore(method.instructions[indices[index]], LdcInsnNode(strings[index]))
            }
        }

        val iter = clinit.instructions.iterator()
        var count = 0
        while (iter.hasNext()){
            val insn = iter.next() as AbstractInsnNode

            if (count == 6)
                break

            if (insn.opcode == Opcodes.SWAP || insn.opcode == Opcodes.POP || insn is FrameNode || insn is TableSwitchInsnNode || insn is LabelNode)
                count++
            else count = 0

            iter.remove()
        }

    }

    private fun specialDecryptClass(classNode: ClassNode){
        var modifiers = ArrayList<Int>()
        var methodIter = classNode.methods.iterator()
        while (modifiers.size == 0 && methodIter.hasNext()){
            modifiers = getModifiers(methodIter.next() as MethodNode)
        }
        if (modifiers.size > 0)
            println(classNode.name)
    }

    private fun decryptString(s : String, modifiers: ArrayList<Int>): String {
        var decrypted = ""
        var i = 0
        for (char in s.toCharArray()){
            val c = (char.toInt() xor modifiers[i%5]).toChar()
            decrypted += c
            i++
        }
        return decrypted
    }

    private fun getModifiers(method: MethodNode): ArrayList<Int>{
        val modifiers = ArrayList<Int>()
        var potentialMatch = false
        val lastInsns = ArrayList<AbstractInsnNode>()

        for (insn in method.instructions) {
            if (insn is AbstractInsnNode) {
                if (insn.opcode == Opcodes.TABLESWITCH) {
                    potentialMatch = true
                }

                if (potentialMatch) {
                    if (insn.opcode != Opcodes.F_NEW && insn.opcode != Opcodes.TABLESWITCH)
                        lastInsns.add(insn)
                    if (!((insn.opcode >= Opcodes.ICONST_0 && insn.opcode <= Opcodes.ICONST_5) ||
                            insn.opcode == Opcodes.BIPUSH || insn.opcode == Opcodes.GOTO ||
                            insn.opcode == Opcodes.F_NEW || insn.opcode == Opcodes.TABLESWITCH)) {
                        potentialMatch = false
                        lastInsns.clear()
                    }
                    if (lastInsns.size > 8) {
                        for (i in 0..4) {
                            modifiers.add(OpUtil.getIntValue(lastInsns[i * 2]))
                        }
                        potentialMatch = false
                        lastInsns.clear()
                    }
                }
            }
        }

        return modifiers
    }
}