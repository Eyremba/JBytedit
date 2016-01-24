package quux.jbytedit.util

import org.objectweb.asm.tree.*
import quux.jbytedit.JBytedit
import quux.jbytedit.tree.ClassTreeNode
import quux.jbytedit.tree.MethodTreeNode
import java.awt.BorderLayout
import java.awt.GridLayout
import java.lang.Float
import javax.swing.*

object Dialog {

    fun error(message: String) {
        JOptionPane.showMessageDialog(JBytedit.INSTANCE,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE)
    }

    fun methodEditor(node :MethodNode){
        val panel = JPanel(BorderLayout(5, 5))
        val input = JPanel(GridLayout(0, 1))
        val labels = JPanel(GridLayout(0, 1))
        panel.add(labels, BorderLayout.WEST)
        panel.add(input, BorderLayout.CENTER)
        labels.add(JLabel("Description: "))
        labels.add(JLabel("ASM Access Code: "))
        labels.add(JLabel("Max Stack Size: "))
        labels.add(JLabel("Max Local Variables: "))
        val desc = JTextField(node.desc)
        val access = JTextField(node.access.toString())
        val maxStack = JTextField(node.maxStack.toString())
        val maxLocals = JTextField(node.maxLocals.toString())
        input.add(desc)
        input.add(access)
        input.add(maxStack)
        input.add(maxLocals)
        val result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Edit Method",
                JOptionPane.OK_CANCEL_OPTION)
        try {
            if (result == JOptionPane.OK_OPTION) {
                node.desc = desc.text
                node.access = Integer.parseInt(access.text)
                node.maxStack = Integer.parseInt(maxStack.text)
                node.maxLocals = Integer.parseInt(maxLocals.text)
            }
        }
        catch (e: Exception){
            JOptionPane.showMessageDialog(JBytedit.INSTANCE,
                    "An error occurred",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    fun fieldEditor(classNode: ClassNode, fieldNode: FieldNode?) {
        var panel = JPanel(BorderLayout(5, 5))
        var input = JPanel(GridLayout(0, 1))
        var labels = JPanel(GridLayout(0, 1))
        panel.add(labels, BorderLayout.WEST)
        panel.add(input, BorderLayout.CENTER)
        labels.add(JLabel("ASM Access Code: "))
        labels.add(JLabel("Name: "))
        labels.add(JLabel("Description: "))
        labels.add(JLabel("Signature: "))
        labels.add(JLabel("Value: "))
        val access = JTextField(fieldNode?.access?.toString()?: "")
        val name = JTextField(fieldNode?.name?.toString()?: "")
        val desc = JTextField(fieldNode?.desc?.toString()?: "")
        val sig = JTextField(fieldNode?.signature?.toString()?: "")
        val value = JTextField(fieldNode?.value?.toString()?: "")
        input.add(access)
        input.add(name)
        input.add(desc)
        input.add(sig)
        input.add(value)
        val promptResult = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Add field", JOptionPane.YES_NO_OPTION)
        if (promptResult == JOptionPane.YES_OPTION) {
            try {
                if (fieldNode != null){
                    fieldNode.access = Integer.parseInt(access.text)
                    fieldNode.name = name.text
                    fieldNode.desc = desc.text
                    fieldNode.signature = sig.text
                    if (value.text.length > 0) {
                        if (desc.text.equals("Ljava/lang/String;"))
                            fieldNode.value = value.text
                        else if (desc.text.equals("I"))
                            fieldNode.value = Integer.parseInt(value.text)
                        else if (desc.text.equals("J"))
                            fieldNode.value = java.lang.Long.parseLong(value.text)
                        else if (desc.text.equals("F"))
                            fieldNode.value = java.lang.Float.parseFloat(value.text)
                        else if (desc.text.equals("D"))
                            fieldNode.value = java.lang.Double.parseDouble(value.text)
                        else
                            throw Exception()
                    } else {
                        fieldNode.value = null
                    }
                }
                else {
                    if (value.text.length > 0) {
                        if (desc.text.equals("Ljava/lang/String;"))
                            classNode.fields.add(FieldNode(Integer.parseInt(access.text), name.text, desc.text, sig.text, value.text))
                        else if (desc.text.equals("I"))
                            classNode.fields.add(FieldNode(Integer.parseInt(access.text), name.text, desc.text, sig.text, Integer.parseInt(value.text)))
                        else if (desc.text.equals("J"))
                            classNode.fields.add(FieldNode(Integer.parseInt(access.text), name.text, desc.text, sig.text, java.lang.Long.parseLong(value.text)))
                        else if (desc.text.equals("F"))
                            classNode.fields.add(FieldNode(Integer.parseInt(access.text), name.text, desc.text, sig.text, java.lang.Float.parseFloat(value.text)))
                        else if (desc.text.equals("D"))
                            classNode.fields.add(FieldNode(Integer.parseInt(access.text), name.text, desc.text, sig.text, java.lang.Double.parseDouble(value.text)))
                        else
                            throw Exception()
                    } else {
                        classNode.fields.add(FieldNode(Integer.parseInt(access.text), name.text, desc.text, sig.text, null))
                    }
                }
            }
            catch (e: Exception){
                Dialog.error("An error occurred")
            }
        }
    }

    fun classAccessEditor(node: ClassNode){
        val panel = JPanel(BorderLayout(5, 5))
        val input = JPanel(GridLayout(0, 1))
        val labels = JPanel(GridLayout(0, 1))
        panel.add(labels, BorderLayout.WEST)
        panel.add(input, BorderLayout.CENTER)
        labels.add(JLabel("ASM Access code: "))
        val code = JTextField(node.access.toString())
        input.add(code)
        val result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Edit Class",
                JOptionPane.OK_CANCEL_OPTION)
        try {
            if (result == JOptionPane.OK_OPTION) {
                node.access = Integer.parseInt(code.text)
            }
        }
        catch (e: Exception){
            Dialog.error("An error occurred")
        }
    }

    fun instructionEditor(method: MethodNode, index: Int){
        val node = method.instructions[index]
        val panel = JPanel(BorderLayout(5, 5))
        val input = JPanel(GridLayout(0, 1))
        val labels = JPanel(GridLayout(0, 1))
        panel.add(labels, BorderLayout.WEST)
        panel.add(input, BorderLayout.CENTER)
        try {
            if (node is FrameNode) { throw UnsupportedOperationException() }
            else if (node is IincInsnNode) { Dialog.iincInsnEditor(node, method, panel, labels, input, null) }
            else if (node is IntInsnNode) { Dialog.intInsnEditor(node, method, panel, labels, input, null) }
            else if (node is InvokeDynamicInsnNode) { throw UnsupportedOperationException() }
            else if (node is JumpInsnNode) { Dialog.jumpInsnEditor(node, method, panel, labels, input, null) }
            else if (node is LdcInsnNode) { Dialog.ldcInsnEditor(node, method, panel, labels, input, null) }
            else if (node is LineNumberNode) { Dialog.lineNumberEditor(node, method, panel, labels, input, null) }
            else if (node is LookupSwitchInsnNode) { throw UnsupportedOperationException() }
            else if (node is MultiANewArrayInsnNode) { throw UnsupportedOperationException() }
            else if (node is TableSwitchInsnNode) { throw UnsupportedOperationException() }
            else if (node is TypeInsnNode) { Dialog.typeInsnEditor(node, method, panel, labels, input, null) }
            else if (node is VarInsnNode) { Dialog.varInsnEditor(node, method, panel, labels, input, null) }
            else if (node is FieldInsnNode) { Dialog.fieldInsnEditor(node, method, panel, labels, input, null) }
            else if (node is MethodInsnNode) { Dialog.methodInsnEditor(node, method, panel, labels, input, null) }
            else if (node is InsnNode) { Dialog.insnEditor(node, method, panel, labels, input, null) }
        }
        catch (e: UnsupportedOperationException){
            Dialog.error("This instruction is not yet supported")
        }
        catch (e: Exception){
            Dialog.error("An error occurred")
        }
    }

    fun insructionInserter(method: MethodNode, index: Int){
        val position = JComboBox(arrayOf("Before", "After"))
        var type = JComboBox(OpUtil.nodeTypes)
        var panel = JPanel(BorderLayout(5, 5))
        var input = JPanel(GridLayout(0, 1))
        var labels = JPanel(GridLayout(0, 1))
        panel.add(labels, BorderLayout.WEST)
        panel.add(input, BorderLayout.CENTER)
        labels.add(JLabel("Position: "))
        input.add(position)
        labels.add(JLabel("Type: "))
        input.add(type)
        var result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Insert node",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.YES_OPTION){
            val target = method.instructions[index - (if (position.selectedItem.equals("Before")) 1 else 0)]
            if (type.selectedItem == "LabelNode"){
                method.instructions.insert(target, LabelNode())
            }
            else {
                panel = JPanel(BorderLayout(5, 5))
                input = JPanel(GridLayout(0, 1))
                labels = JPanel(GridLayout(0, 1))
                panel.add(labels, BorderLayout.WEST)
                panel.add(input, BorderLayout.CENTER)
                try {
                    when (type.selectedItem) {
                        "FrameNode" -> throw UnsupportedOperationException()
                        "IincInsnNode" -> Dialog.iincInsnEditor(null, method, panel, labels, input, target)
                        "IntInsnNode" -> Dialog.intInsnEditor(null, method, panel, labels, input, target)
                        "InvokeDynamicInsnNode" -> throw UnsupportedOperationException()
                        "JumpInsnNode" -> Dialog.jumpInsnEditor(null, method, panel, labels, input, target)
                        "LdcInsnNode" -> Dialog.ldcInsnEditor(null, method, panel, labels, input, target)
                        "LineNumberNode" -> Dialog.lineNumberEditor(null, method, panel, labels, input, target)
                        "LookupSwitchInsnNode" -> throw UnsupportedOperationException()
                        "MultiANewArrayInsnNode" -> throw UnsupportedOperationException()
                        "TableSwitchInsnNode" -> throw UnsupportedOperationException()
                        "TypeInsnNode" -> Dialog.typeInsnEditor(null, method, panel, labels, input, target)
                        "VarInsnNode" -> Dialog.varInsnEditor(null, method, panel, labels, input, target)
                        "FieldInsnNode" -> Dialog.fieldInsnEditor(null, method, panel, labels, input, target)
                        "MethodInsnNode" -> Dialog.methodInsnEditor(null, method, panel, labels, input, target)
                        "InsnNode" -> Dialog.insnEditor(null, method, panel, labels, input, target)
                    }
                }
                catch (e: UnsupportedOperationException){
                    Dialog.error("This instruction is not yet supported")
                }
                catch (e: Exception){
                    Dialog.error("An error occurred")
                }
            }
            Populator.populateInstructionsList(method)
        }
    }

    fun iincInsnEditor(node: IincInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, target: AbstractInsnNode?) {
        labels.add(JLabel("Local variable index: "))
        val index = JTextField()
        input.add(index)
        labels.add(JLabel("Increment by: "))
        val value = JTextField()
        input.add(value)
        val result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Insert Iinc Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            method.instructions.insert(target, IincInsnNode(Integer.parseInt(index.text), Integer.parseInt(value.text)))
        }
    }

    fun intInsnEditor(node: IntInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, target: AbstractInsnNode?) {
        labels.add(JLabel("Type: "))
        val insn = JComboBox(arrayOf("bipush", "sipush", "newarray"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode?: 0]
        input.add(insn)
        labels.add(JLabel("Operand: "))
        val operand = JTextField(node?.operand?.toString() ?: "")
        labels.add(operand)
        val result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Insert Int Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            if (node == null)
                method.instructions.insert(target, IntInsnNode(opcode, Integer.parseInt(operand.text)))
            else {
                method.instructions.insert(node, IntInsnNode(opcode, Integer.parseInt(operand.text)))
                method.instructions.remove(node)
            }
        }
    }

    fun jumpInsnEditor(node: JumpInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, target: AbstractInsnNode?) {
        labels.add(JLabel("Type: "))
        val insn = JComboBox(arrayOf("ifeq", "ifne", "iflt", "ifge", "ifgt",
                "ifle", "if_icmpeq", "if_icmpne", "if_icmplt", "if_icmpge",
                "if_icmpgt", "if_icmple", "if_acmpeq", "if_acmpne", "goto",
                "jsr", "ifnull", "ifnonnull"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode ?: 0]
        input.add(insn)
        labels.add(JLabel("Label number: "))
        var count = 0
        if (node != null)
            for (insn2 in method.instructions){
                if (insn2 is LabelNode){
                    if (insn2 == node.label){
                        break
                    }
                    count++
                }
            }
        var label = JTextField(if (count == 0) "" else count.toString())
        input.add(label)
        val result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Insert Jump Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            val labelIndex = Integer.parseInt(label.text)
            var labelNode: LabelNode? = null
            var count2 = 0
            for (insn2 in method.instructions){
                if (insn2 is LabelNode){
                    if (count2 == labelIndex){
                        labelNode = insn2
                        break
                    }
                    count2++
                }
            }
            if (labelNode == null){
                throw Exception()
            }
            else {
                if (node == null)
                    method.instructions.insert(target, JumpInsnNode(opcode, labelNode))
                else {
                    method.instructions.insert(node, JumpInsnNode(opcode, labelNode))
                    method.instructions.remove(node)
                }
            }
        }
    }

    fun ldcInsnEditor(node: LdcInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, target: AbstractInsnNode?) {
        labels.add(JLabel("Type: "))
        val insn = JComboBox(arrayOf("String", "int", "float"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode?: 0]
        input.add(insn)
        labels.add(JLabel("Value: "))
        val value = JTextField(node?.cst?.toString() ?: "")
        input.add(value)
        val result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Insert LDC Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            if (node == null) {
                when (insn.selectedItem as String) {
                    "String" -> method.instructions.insert(target, LdcInsnNode(value.text))
                    "int" -> method.instructions.insert(target, LdcInsnNode(Integer.parseInt(value.text)))
                    "float" -> method.instructions.insert(target, LdcInsnNode(Float.parseFloat(value.text)))
                }
            } else {
                when (insn.selectedItem as String) {
                    "String" -> method.instructions.insert(node, LdcInsnNode(value.text))
                    "int" -> method.instructions.insert(node, LdcInsnNode(Integer.parseInt(value.text)))
                    "float" -> method.instructions.insert(node, LdcInsnNode(Float.parseFloat(value.text)))
                }
                method.instructions.remove(node)
            }
        }
    }

    fun lineNumberEditor(node: LineNumberNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, target: AbstractInsnNode?) {
        labels.add(JLabel("Line number: "))
        var value = JTextField(node?.line?.toString() ?: "")
        input.add(value)
        val result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Insert Line Number",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            val label = LabelNode()
            if (node == null) {
                method.instructions.insert(target, LineNumberNode(Integer.parseInt(value.text), label))
                method.instructions.insert(target, label)
            } else {
                node.line = Integer.parseInt(value.text)
            }
        }
    }

    fun typeInsnEditor(node: TypeInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, target: AbstractInsnNode?) {
        labels.add(JLabel("Instruction: "))
        val insn = JComboBox(arrayOf("new", "anewarray", "checkcast", "instanceof"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode?: 0]
        input.add(insn)
        labels.add(JLabel("Type description: "))
        val desc = JTextField(node?.desc ?: "")
        input.add(desc)
        val result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Insert Type Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            if (node == null)
                method.instructions.insert(target, TypeInsnNode(opcode, desc.text))
            else {
                method.instructions.insert(node, TypeInsnNode(opcode, desc.text))
                method.instructions.remove(node)
            }
        }
    }

    fun varInsnEditor(node: VarInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, target: AbstractInsnNode?) {
        labels.add(JLabel("Type: "))
        val insn= JComboBox(arrayOf("iload", "lload", "fload", "dload",
                "aload", "istore", "lstore", "fstore", "dstore", "astore", "ret"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode?: 0]
        input.add(insn)
        labels.add(JLabel("Local variable index: "))
        val index = JTextField(node?.`var`?.toString() ?: "")
        input.add(index)
        val result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Insert Var Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            if (node == null)
                method.instructions.insert(target, VarInsnNode(opcode, Integer.parseInt(index.text)))
            else {
                method.instructions.insert(node, VarInsnNode(opcode, Integer.parseInt(index.text)))
                method.instructions.remove(node)
            }
        }
    }

    fun fieldInsnEditor(node: FieldInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, target: AbstractInsnNode?) {
        labels.add(JLabel("Type: "))
        val insn= JComboBox(arrayOf("getstatic", "putstatic", "getfield", "putfield"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode?: 0]
        input.add(insn)
        labels.add(JLabel("Owner: "))
        val owner = JTextField(node?.owner ?: "")
        input.add(owner)
        labels.add(JLabel("Name: "))
        val name = JTextField(node?.name ?: "")
        input.add(name)
        labels.add(JLabel("Description: "))
        val desc = JTextField(node?.desc ?: "")
        input.add(desc)
        val result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Insert Field Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            if (node == null)
                method.instructions.insert(target, FieldInsnNode(opcode, owner.text, name.text, desc.text))
            else {
                method.instructions.insert(node, FieldInsnNode(opcode, owner.text, name.text, desc.text))
                method.instructions.remove(node)
            }
        }
    }

    fun methodInsnEditor(node: MethodInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, target: AbstractInsnNode?) {
        labels.add(JLabel("Type: "))
        val insn = JComboBox(arrayOf("invokestatic", "invokevirtual", "invokespecial", "invokeinterface"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode?: 0]
        input.add(insn)
        labels.add(JLabel("Owner: "))
        val owner = JTextField(node?.owner ?: "")
        input.add(owner)
        labels.add(JLabel("Name: "))
        val name = JTextField(node?.name ?: "")
        input.add(name)
        labels.add(JLabel("Description: "))
        val desc = JTextField(node?.desc ?: "")
        input.add(desc)
        val result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Insert Method Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            if (node == null)
                method.instructions.insert(target, MethodInsnNode(opcode, owner.text, name.text, desc.text))
            else {
                method.instructions.insert(node, MethodInsnNode(opcode, owner.text, name.text, desc.text))
                method.instructions.remove(node)
            }
        }
    }

    fun insnEditor(node: InsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, target: AbstractInsnNode?) {
        labels.add(JLabel("Type: "))
        val insn = JComboBox(arrayOf("nop", "aconst_null", "iconst_m1",
                "iconst_0", "iconst_1", "iconst_2", "iconst_3", "iconst_4",
                "iconst_5", "lconst_0", "lconst_1", "fconst_0", "fconst_1",
                "fconst_2", "dconst_0", "dconst_1", "iaload", "laload",
                "faload", "daload", "aaload", "baload", "caload", "saload",
                "iastore", "lastore", "fastore", "dastore", "aastore",
                "bastore", "castore", "sastore", "pop", "pop2", "dup",
                "dup_x1", "dup_x2", "dup2", "dup2_x1", "dup2_x2", "swap",
                "iadd", "ladd", "fadd", "dadd", "isub", "lsub", "fsub",
                "dsub", "imul", "lmul", "fmul", "dmul", "idiv", "ldiv",
                "fdiv", "ddiv", "irem", "lrem", "frem", "drem", "ineg",
                "lneg", "fneg", "dneg", "ishl", "lshl", "ishr", "lshr",
                "iushr", "lushr", "iand", "land", "ior", "lor", "ixor",
                "lxor", "i2l", "i2f", "i2d", "l2i", "l2f", "l2d", "f2i",
                "f2l", "f2d", "d2i", "d2l", "d2f", "i2b", "i2c", "i2s",
                "lcmp", "fcmpl", "fcmpg", "dcmpl", "dcmpg", "ireturn",
                "lreturn", "freturn", "dreturn", "areturn", "return",
                "arraylength", "athrow", "monitorenter", "monitorexit"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode?: 0]
        input.add(insn)
        val result = JOptionPane.showConfirmDialog(JBytedit.INSTANCE, panel, "Insert Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            if (node == null)
                method.instructions.insert(target, InsnNode(opcode))
            else{
                method.instructions.insert(node, InsnNode(opcode))
                method.instructions.remove(node)
            }
        }
    }

}