package quux.jbytedit.decrypt

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import quux.jbytedit.util.OpUtil
import java.util.*

object ZKMDecrypter {

    @Suppress("UNCHECKED_CAST") fun decryptClass(classNode: ClassNode): ArrayList<String> {

        val encList = ArrayList<String>()
        val list = ArrayList<String>()

        var clinit: MethodNode? = null
        for (method in classNode.methods){
            if (method is MethodNode){
                if (method.name.equals("<clinit>")){
                    clinit = method
                }
            }
        }

        if (clinit == null){
            return list
        }

        for (insn in clinit.instructions) {
            if (insn is AbstractInsnNode) {
                if (insn is LdcInsnNode && insn.cst is String) {
                    encList.add(insn.cst as String)
                }
            }
        }

        var modifiers = getModifiers(clinit)
        var methodIter = classNode.methods.iterator()
        while (modifiers.size == 0 && methodIter.hasNext()){
            modifiers = getModifiers(methodIter.next() as MethodNode)
            if (modifiers.size != 0)
                println(classNode.name)
        }

        if (modifiers.size == 0) {
            return list
        }

        encList.forEach { list.add(decrypt(it, modifiers)) }
        return list
    }

    private fun decrypt(s : String, modifiers: ArrayList<Int>): String {
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