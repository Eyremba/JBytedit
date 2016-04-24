package quux.jbytedit.util

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.util.*

object OpUtil {

    val mnemonics = mapOf(0 to "nop", 1 to "aconst_null", 2 to "iconst_m1", 3 to "iconst_0", 4 to "iconst_1",
            5 to "iconst_2", 6 to "iconst_3", 7 to "iconst_4", 8 to "iconst_5", 9 to "lconst_0", 10 to "lconst_1",
            11 to "fconst_0", 12 to "fconst_1", 13 to "fconst_2", 14 to "dconst_0", 15 to "dconst_1", 16 to "bipush",
            17 to "sipush", 18 to "ldc", 21 to "iload", 22 to "lload", 23 to "fload", 24 to "dload", 25 to "aload",
            32 to "acc_synchronized", 46 to "iaload", 47 to "laload", 48 to "faload", 49 to "daload", 50 to "aaload",
            51 to "baload", 52 to "caload", 53 to "saload", 54 to "istore", 55 to "lstore", 56 to "fstore",
            57 to "dstore", 58 to "astore", 64 to "acc_bridge", 79 to "iastore", 80 to "lastore", 81 to "fastore",
            82 to "dastore", 83 to "aastore", 84 to "bastore", 85 to "castore", 86 to "sastore", 87 to "pop",
            88 to "pop2", 89 to "dup", 90 to "dup_x1", 91 to "dup_x2", 92 to "dup2", 93 to "dup2_x1", 94 to "dup2_x2",
            95 to "swap", 96 to "iadd", 97 to "ladd", 98 to "fadd", 99 to "dadd", 100 to "isub", 101 to "lsub",
            102 to "fsub", 103 to "dsub", 104 to "imul", 105 to "lmul", 106 to "fmul", 107 to "dmul", 108 to "idiv",
            109 to "ldiv", 110 to "fdiv", 111 to "ddiv", 112 to "irem", 113 to "lrem", 114 to "frem", 115 to "drem",
            116 to "ineg", 117 to "lneg", 118 to "fneg", 119 to "dneg", 120 to "ishl", 121 to "lshl", 122 to "ishr",
            123 to "lshr", 124 to "iushr", 125 to "lushr", 126 to "iand", 127 to "land", 128 to "ior", 129 to "lor",
            130 to "ixor", 131 to "lxor", 132 to "iinc", 133 to "i2l", 134 to "i2f", 135 to "i2d", 136 to "l2i",
            137 to "l2f", 138 to "l2d", 139 to "f2i", 140 to "f2l", 141 to "f2d", 142 to "d2i", 143 to "d2l",
            144 to "d2f", 145 to "i2b", 146 to "i2c", 147 to "i2s", 148 to "lcmp", 149 to "fcmpl", 150 to "fcmpg",
            151 to "dcmpl", 152 to "dcmpg", 153 to "ifeq", 154 to "ifne", 155 to "iflt", 156 to "ifge", 157 to "ifgt",
            158 to "ifle", 159 to "if_icmpeq", 160 to "if_icmpne", 161 to "if_icmplt", 162 to "if_icmpge",
            163 to "if_icmpgt", 164 to "if_icmple", 165 to "if_acmpeq", 166 to "if_acmpne", 167 to "goto", 168 to "jsr",
            169 to "ret", 170 to "tableswitch", 171 to "lookupswitch", 172 to "ireturn", 173 to "lreturn",
            174 to "freturn", 175 to "dreturn", 176 to "areturn", 177 to "return", 178 to "getstatic",
            179 to "putstatic", 180 to "getfield", 181 to "putfield", 182 to "invokevirtual", 183 to "invokespecial",
            184 to "invokestatic", 185 to "invokeinterface", 186 to "invokedynamic", 187 to "new", 188 to "newarray",
            189 to "anewarray", 190 to "arraylength", 191 to "athrow", 192 to "checkcast", 193 to "instanceof",
            194 to "monitorenter", 195 to "monitorexit", 197 to "multianewarray", 198 to "ifnull", 199 to "ifnonnull")

    val opcodes = HashMap<String, Int>()


    init {
        mnemonics.keys.forEach { opcodes.put(mnemonics[it]!!, it) }
    }

    val nodeTypes = arrayOf("IincInsnNode", "IntInsnNode",
            "JumpInsnNode", "LabelNode", "LdcInsnNode",
            "LineNumberNode", "TypeInsnNode", "VarInsnNode", "FieldInsnNode",
            "MethodInsnNode", "InsnNode")//"FrameNode", "InvokeDynamicInsnNode", "LookupSwitchInsnNode", "MultiANewArrayInsnNode", "TableSwitchInsnNode",

    val resolvedLabels = HashMap<Int, Int>()
    var labelCount = 0

    fun resetLabels() {
        resolvedLabels.clear()
        labelCount = 0
    }

    fun getDisplayArgs(args: String): String {
        val rawArgs = args.split(")").first().substring(1)
        val result = getDisplayType(rawArgs)
        return result
    }

    fun getDisplayType(rawType: String): String {
        var result = ""
        var tmpArg = ""
        var argSuffix = ""
        var isFullyQualifiedClass = false
        for (char in rawType.toCharArray()) {
            if (isFullyQualifiedClass) {
                if (char == ';') {
                    result += tmpArg.split("/").last() + argSuffix + ", "
                    argSuffix = ""
                    isFullyQualifiedClass = false
                    tmpArg = ""
                } else {
                    tmpArg += char
                }
            } else if (char == '[') {
                argSuffix += "[]"
            } else if (char == 'L') {
                isFullyQualifiedClass = true
            } else {
                if (char == 'Z') {
                    result += "boolean"
                } else if (char == 'B') {
                    result += "byte"
                } else if (char == 'C') {
                    result += "char"
                } else if (char == 'S') {
                    result += "short"
                } else if (char == 'I') {
                    result += "int"
                } else if (char == 'J') {
                    result += "long"
                } else if (char == 'F') {
                    result += "float"
                } else if (char == 'D') {
                    result += "double"
                } else if (char == 'V') {
                    result += "void"
                } else {
                    isFullyQualifiedClass = true
                    continue
                }

                result += argSuffix
                argSuffix = ""
                result += ", "
            }
        }

        if (tmpArg.length != 0) {
            result += tmpArg.split("/").last() + argSuffix + ", "
        }

        if (result.length >= 2) {
            result = result.substring(0, result.lastIndex - 1)
        }
        return TextUtil.addTag(result, "font color=#557799")
    }

    fun getDisplayClass(fullName: String): String {
        return TextUtil.addTag(fullName.split("/").last(), "font color=#557799")
    }

    fun getDisplayAccess(access: Int): String {
        var result = ""
        if (access and Opcodes.ACC_PUBLIC != 0) {
            result += "public "
        }
        if (access and Opcodes.ACC_PRIVATE != 0) {
            result += "private "
        }
        if (access and Opcodes.ACC_PROTECTED != 0) {
            result += "protected "
        }
        if (access and Opcodes.ACC_STATIC != 0) {
            result += "static "
        }
        if (access and Opcodes.ACC_FINAL != 0) {
            result += "final "
        }
        if (result.length > 0) {
            result = result.substring(0, result.lastIndex)
        }
        return result
    }

    private fun getInstructionDisplayContent(node: AbstractInsnNode): String {
        return TextUtil.toBold(mnemonics[node.opcode]) +
        if (node is FrameNode) {
            return TextUtil.toLighter("stack frame")
        } else if (node is IincInsnNode) {
            " " + node.`var` + " " + node.incr
        } else if (node is IntInsnNode) {
            " ${node.operand}"
        } else if (node is InvokeDynamicInsnNode) {
            " ${node.name}(${getDisplayArgs(node.desc)}) - unsupported dynamic method! "
        } else if (node is JumpInsnNode) {
            " " + getLabelNumber(node.label)
        } else if (node is LabelNode) {
            return TextUtil.toLighter("label " + getLabelNumber(node))
        } else if (node is LdcInsnNode) {
            if (node.cst is String)
                " " + TextUtil.addTag("\"${node.cst}\"", "font color=#559955")
            else
                " ${node.cst}"
        } else if (node is LineNumberNode) {
            return TextUtil.toLighter("line " + node.line)
        } else if (node is LookupSwitchInsnNode) {
            return TextUtil.toLighter("lookup switch")
        } else if (node is MultiANewArrayInsnNode) {
            " - unsuported MiltiArray node!"
        } else if (node is TableSwitchInsnNode) {
            return TextUtil.toLighter("table switch")
        } else if (node is TypeInsnNode) {
            " " + getDisplayClass(node.desc)
        } else if (node is VarInsnNode) {
            " " + node.`var`
        } else if (node is FieldInsnNode) {
            " ${getDisplayType(node.desc)} ${node.owner.split("/").last()}.${node.name}"
        } else if (node is MethodInsnNode) {
            " " + getDisplayType(node.desc.split(")")[1]) + " ${TextUtil.addTag(node.owner.split("/").last(), "font color=#995555")}.${TextUtil.escapeHTML(node.name)}(${getDisplayArgs(node.desc)})"

        } else {
            return TextUtil.toBold(mnemonics[node.opcode])
        }
    }

    fun getDisplayInstruction(node: AbstractInsnNode): String {
        return TextUtil.toHtml(getInstructionDisplayContent(node))
    }

    fun getLabelNumber(node: LabelNode): Int{
        var label = 0
        if (resolvedLabels.contains(node.label.hashCode())){
            label = resolvedLabels[node.label.hashCode()]!!
        }
        else {
            resolvedLabels.put(node.label.hashCode(), labelCount)
            label = labelCount
            labelCount ++
        }
        return label
    }

    fun getDisplayField(field: FieldNode): String {
        return TextUtil.toHtml(
                OpUtil.getDisplayAccess(field.access) + " " +
                        TextUtil.addTag(OpUtil.getDisplayType(field.desc) + " " + field.name, "b") + " = " +
                        (if (field.value is String) TextUtil.addTag("\"${field.value}\"", "font color=#559955")
                        else TextUtil.addTag(field.value?.toString() ?: "null", "font color=#aa5555")) + ";")
    }

    fun getIntValue(insn: AbstractInsnNode): Int {
        if (insn.opcode == Opcodes.ICONST_0) { return 0 }
        else if (insn.opcode == Opcodes.ICONST_M1) { return -1 }
        else if (insn.opcode == Opcodes.ICONST_1) { return 1 }
        else if (insn.opcode == Opcodes.ICONST_2) { return 2 }
        else if (insn.opcode == Opcodes.ICONST_3) { return 3 }
        else if (insn.opcode == Opcodes.ICONST_4) { return 4 }
        else if (insn.opcode == Opcodes.ICONST_5) { return 5 }
        else if (insn is IntInsnNode){ return insn.operand }
        else return Int.MIN_VALUE
    }

}