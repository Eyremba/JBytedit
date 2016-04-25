package quux.jbytedit.util

import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*

class MyInterpreter: Interpreter(327680), Opcodes {

    override fun newValue(var1: Type?): BasicValue? {
        if (var1 == null) {
            return BasicValue.UNINITIALIZED_VALUE
        } else {
            when (var1.sort) {
                0 -> return null
                1, 2, 3, 4, 5 -> return BasicValue.INT_VALUE
                6 -> return BasicValue.FLOAT_VALUE
                7 -> return BasicValue.LONG_VALUE
                8 -> return BasicValue.DOUBLE_VALUE
                9, 10 -> return BasicValue(var1)
                else -> throw Error("Internal error")
            }
        }
    }

    @Throws(AnalyzerException::class)
    override fun newOperation(var1: AbstractInsnNode): Value? {
        when (var1.opcode) {
            1 -> return this.newValue(Type.getObjectType("null"))
            2, 3, 4, 5, 6, 7, 8 -> return BasicValue.INT_VALUE
            9, 10 -> return BasicValue.LONG_VALUE
            11, 12, 13 -> return BasicValue.FLOAT_VALUE
            14, 15 -> return BasicValue.DOUBLE_VALUE
            16, 17 -> return BasicValue.INT_VALUE
            18 -> {
                val var2 = (var1 as LdcInsnNode).cst
                if (var2 is Int) {
                    return BasicValue.INT_VALUE
                } else if (var2 is Float) {
                    return BasicValue.FLOAT_VALUE
                } else if (var2 is Long) {
                    return BasicValue.LONG_VALUE
                } else if (var2 is Double) {
                    return BasicValue.DOUBLE_VALUE
                } else if (var2 is String) {
                    return this.newValue(Type.getObjectType("java/lang/String"))
                } else if (var2 is Type) {
                    val var3 = var2.sort
                    if (var3 != 10 && var3 != 9) {
                        if (var3 == 11) {
                            return this.newValue(Type.getObjectType("java/lang/invoke/MethodType"))
                        }

                        throw IllegalArgumentException("Illegal LDC constant " + var2)
                    }

                    return this.newValue(Type.getObjectType("java/lang/Class"))
                } else {
                    if (var2 is Handle) {
                        return this.newValue(Type.getObjectType("java/lang/invoke/MethodHandle"))
                    }

                    throw IllegalArgumentException("Illegal LDC constant " + var2)
                }
            }
            168 -> return BasicValue.RETURNADDRESS_VALUE
            178 -> return this.newValue(Type.getType((var1 as FieldInsnNode).desc))
            187 -> return this.newValue(Type.getObjectType((var1 as TypeInsnNode).desc))
            else -> throw Error("Internal error.")
        }
    }

    @Throws(AnalyzerException::class)
    override fun copyOperation(var1: AbstractInsnNode, var2: Value): Value {
        return var2
    }

    @Throws(AnalyzerException::class)
    override fun unaryOperation(var1: AbstractInsnNode, var2: Value): Value? {
        val var3: String
        when (var1.opcode) {
            116, 132, 136, 139, 142, 145, 146, 147 -> return BasicValue.INT_VALUE
            117, 133, 140, 143 -> return BasicValue.LONG_VALUE
            118, 134, 137, 144 -> return BasicValue.FLOAT_VALUE
            119, 135, 138, 141 -> return BasicValue.DOUBLE_VALUE
            153, 154, 155, 156, 157, 158, 170, 171, 172, 173, 174, 175, 176, 179 -> return null
            180 -> return this.newValue(Type.getType((var1 as FieldInsnNode).desc))
            188 -> {
                when ((var1 as IntInsnNode).operand) {
                    4 -> return this.newValue(Type.getType("[Z"))
                    5 -> return this.newValue(Type.getType("[C"))
                    6 -> return this.newValue(Type.getType("[F"))
                    7 -> return this.newValue(Type.getType("[D"))
                    8 -> return this.newValue(Type.getType("[B"))
                    9 -> return this.newValue(Type.getType("[S"))
                    10 -> return this.newValue(Type.getType("[I"))
                    11 -> return this.newValue(Type.getType("[J"))
                    else -> throw AnalyzerException(var1, "Invalid array type")
                }
            }
            189 -> {
                var3 = (var1 as TypeInsnNode).desc
                return this.newValue(Type.getType("[" + Type.getObjectType(var3)))
            }
            190 -> return BasicValue.INT_VALUE
            191 -> return null
            192 -> {
                var3 = (var1 as TypeInsnNode).desc
                return this.newValue(Type.getObjectType(var3))
            }
            193 -> return BasicValue.INT_VALUE
            194, 195, 198, 199 -> return null
            else -> throw Error("Internal error.")
        }
    }

    @Throws(AnalyzerException::class)
    override fun binaryOperation(var1: AbstractInsnNode, var2: Value, var3: Value): Value? {
        when (var1.opcode) {
            46, 51, 52, 53, 96, 100, 104, 108, 112, 120, 122, 124, 126, 128, 130 -> return BasicValue.INT_VALUE
            47, 97, 101, 105, 109, 113, 121, 123, 125, 127, 129, 131 -> return BasicValue.LONG_VALUE
            48, 98, 102, 106, 110, 114 -> return BasicValue.FLOAT_VALUE
            49, 99, 103, 107, 111, 115 -> return BasicValue.DOUBLE_VALUE
            50 -> {
                return BasicValue((var2 as BasicValue).type.elementType)
            }
            148, 149, 150, 151, 152 -> return BasicValue.INT_VALUE
            159, 160, 161, 162, 163, 164, 165, 166, 181 -> return null
            else -> throw Error("internal error")
        }
    }

    @Throws(AnalyzerException::class)
    override fun ternaryOperation(var1: AbstractInsnNode, var2: Value, var3: Value, var4: Value): Value? {
        return null
    }

    override fun naryOperation(p0: AbstractInsnNode, p1: MutableList<Any?>): Value? {
        val var3 = p0.opcode
        return if (var3 == 197) this.newValue(Type.getType((p0 as MultiANewArrayInsnNode).desc)) else if (var3 == 186) this.newValue(Type.getReturnType((p0 as InvokeDynamicInsnNode).desc)) else this.newValue(Type.getReturnType((p0 as MethodInsnNode).desc))

    }

    @Throws(AnalyzerException::class)
    override fun returnOperation(var1: AbstractInsnNode, var2: Value, var3: Value) {
    }

    override fun merge(var1: Value, var2: Value): Value {
        return if (var1 != var2) BasicValue.UNINITIALIZED_VALUE else var1
    }
}