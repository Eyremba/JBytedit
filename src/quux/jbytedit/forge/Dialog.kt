package quux.jbytedit.forge

import org.objectweb.asm.tree.*
import quux.jbytedit.JBytedit
import quux.jbytedit.entry.SearchEntry
import quux.jbytedit.render.BlankListItem
import quux.jbytedit.util.Edit
import quux.jbytedit.util.FileUtil
import quux.jbytedit.util.OpUtil
import quux.jbytedit.util.TextUtil
import java.awt.BorderLayout
import java.awt.GridLayout
import java.util.*
import javax.swing.*

object Dialog {

    fun error(message: String) {
        JOptionPane.showMessageDialog(JBytedit,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE)
    }

    fun confirm(message: String): Boolean {
        return JOptionPane.showConfirmDialog(quux.jbytedit.JBytedit,
                message,
                "Confirmation needed", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION
    }

    fun methodEditor(node: MethodNode) {
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
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Edit Method",
                JOptionPane.OK_CANCEL_OPTION)
        try {
            if (result == JOptionPane.OK_OPTION) {
                node.desc = desc.text
                node.access = Integer.parseInt(access.text)
                node.maxStack = Integer.parseInt(maxStack.text)
                node.maxLocals = Integer.parseInt(maxLocals.text)
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(JBytedit,
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
        val access = JTextField(fieldNode?.access?.toString() ?: "")
        val name = JTextField(fieldNode?.name?.toString() ?: "")
        val desc = JTextField(fieldNode?.desc?.toString() ?: "")
        val sig = JTextField(fieldNode?.signature?.toString() ?: "")
        val value = JTextField(fieldNode?.value?.toString() ?: "")
        input.add(access)
        input.add(name)
        input.add(desc)
        input.add(sig)
        input.add(value)
        val promptResult = JOptionPane.showConfirmDialog(JBytedit, panel, "Add field", JOptionPane.YES_NO_OPTION)
        if (promptResult == JOptionPane.YES_OPTION) {
            try {
                val field = fieldNode ?: FieldNode(0, null, null, null, null)
                field.access = Integer.parseInt(access.text)
                field.name = name.text
                field.desc = desc.text
                field.signature = sig.text
                if (value.text.length > 0) {
                    if (desc.text.equals("Ljava/lang/String;"))
                        field.value = value.text
                    else if (desc.text.equals("I"))
                        field.value = Integer.parseInt(value.text)
                    else if (desc.text.equals("J"))
                        field.value = java.lang.Long.parseLong(value.text)
                    else if (desc.text.equals("F"))
                        field.value = java.lang.Float.parseFloat(value.text)
                    else if (desc.text.equals("D"))
                        field.value = java.lang.Double.parseDouble(value.text)
                    else
                        throw Exception()
                } else {
                    field.value = null
                }
                if (fieldNode == null) {
                    classNode.fields.add(field)
                }
            } catch (e: Exception) {
                Dialog.error("An error occurred")
            }
        }
    }

    fun classAccessEditor(node: ClassNode) {
        val panel = JPanel(BorderLayout(5, 5))
        val input = JPanel(GridLayout(0, 1))
        val labels = JPanel(GridLayout(0, 1))
        panel.add(labels, BorderLayout.WEST)
        panel.add(input, BorderLayout.CENTER)
        labels.add(JLabel("ASM Access code: "))
        val code = JTextField(node.access.toString())
        input.add(code)
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Edit Class",
                JOptionPane.OK_CANCEL_OPTION)
        try {
            if (result == JOptionPane.OK_OPTION) {
                node.access = Integer.parseInt(code.text)
            }
        } catch (e: Exception) {
            Dialog.error("An error occurred")
        }
    }

    fun instructionEditor(method: MethodNode, index: Int) {
        val node = method.instructions[index]
        abstractInsnEditor(node.javaClass.simpleName, node, method, true)

    }

    fun insertInstruction(method: MethodNode, parent: ClassNode, index: Int) {
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
        var result = JOptionPane.showConfirmDialog(JBytedit, panel, "Insert node",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.YES_OPTION) {
            val modifiedIndex = index - (if (position.selectedItem.equals("Before")) 1 else 0)
            var target: AbstractInsnNode?
            if (modifiedIndex == -1) {
                target = InsnNode(-1)
                JBytedit.insnListModel!!.add(0, BlankListItem())
                method.instructions.insertBefore(method.instructions.first, target)
            } else {
                target = method.instructions[modifiedIndex]
            }
            if (type.selectedItem == "LabelNode") {
                val labelNode = LabelNode()
                method.instructions.insert(target, labelNode)
                JBytedit.openMethod(method, parent, index)
            } else {
                abstractInsnEditor(type.selectedItem.toString(), target, method, false)
            }
            if (type.selectedItem == "LineNumberNode"){
                JBytedit.openMethod(method, parent, index)
            }
            if (modifiedIndex == -1) {
                JBytedit.insnListModel!!.removeElementAt(method.instructions.indexOf(target))
                method.instructions.remove(target)
            }
        }
    }

    fun abstractInsnEditor(type: String, node: AbstractInsnNode?, method: MethodNode, edit: Boolean)
    {
        val panel = JPanel(BorderLayout(5, 5))
        val input = JPanel(GridLayout(0, 1))
        val labels = JPanel(GridLayout(0, 1))
        panel.add(labels, BorderLayout.WEST)
        panel.add(input, BorderLayout.CENTER)
        try {
            when (type) {
                "FrameNode" -> throw UnsupportedOperationException()
                "IincInsnNode" -> iincInsnEditor(node, method, panel, labels, input, edit)
                "IntInsnNode" -> intInsnEditor(node, method, panel, labels, input, edit)
                "InvokeDynamicInsnNode" -> throw UnsupportedOperationException()
                "JumpInsnNode" -> jumpInsnEditor(node, method, panel, labels, input, edit)
                "LdcInsnNode" -> ldcInsnEditor(node, method, panel, labels, input, edit)
                "LineNumberNode" -> lineNumberEditor(node, method, panel, labels, input, edit)
                "LookupSwitchInsnNode" -> throw UnsupportedOperationException()
                "MultiANewArrayInsnNode" -> throw UnsupportedOperationException()
                "TableSwitchInsnNode" -> throw UnsupportedOperationException()
                "TypeInsnNode" -> typeInsnEditor(node, method, panel, labels, input, edit)
                "VarInsnNode" -> varInsnEditor(node, method, panel, labels, input, edit)
                "FieldInsnNode" -> fieldInsnEditor(node, method, panel, labels, input, edit)
                "MethodInsnNode" -> methodInsnEditor(node, method, panel, labels, input, edit)
                "InsnNode" -> insnEditor(node, method, panel, labels, input, edit)
            }
        } catch (e: UnsupportedOperationException) {
            Dialog.error("This instruction is not yet supported")
        } catch (e: Exception) {
            Dialog.error("An error occurred")
        }
    }

    fun iincInsnEditor(node: AbstractInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, edit: Boolean) {
        labels.add(JLabel("Local variable index: "))
        val index = JTextField(if (node is IincInsnNode) node.`var`.toString() else "")
        input.add(index)
        labels.add(JLabel("Increment by: "))
        val value = JTextField(if (node is IincInsnNode) node.incr.toString() else "")
        input.add(value)
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Insert Iinc Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            Edit.insertOrReplaceInsn(IincInsnNode(Integer.parseInt(index.text), Integer.parseInt(value.text)), node, method.instructions, edit)
        }
    }

    fun intInsnEditor(node: AbstractInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, edit: Boolean) {
        labels.add(JLabel("Type: "))
        val insn = JComboBox(arrayOf("bipush", "sipush", "newarray"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode ?: 0]
        input.add(insn)
        labels.add(JLabel("Operand: "))
        val operand = JTextField(if (node is IntInsnNode) node.operand.toString() else "")
        labels.add(operand)
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Insert Int Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            Edit.insertOrReplaceInsn(IntInsnNode(opcode, Integer.parseInt(operand.text)), node, method.instructions, edit)
        }
    }

    fun jumpInsnEditor(node: AbstractInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, edit: Boolean) {
        val labelInsnNode = (if (node is LabelNode) node else null)
        labels.add(JLabel("Type: "))
        val insn = JComboBox(arrayOf("ifeq", "ifne", "iflt", "ifge", "ifgt",
                "ifle", "if_icmpeq", "if_icmpne", "if_icmplt", "if_icmpge",
                "if_icmpgt", "if_icmple", "if_acmpeq", "if_acmpne", "goto",
                "jsr", "ifnull", "ifnonnull"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode ?: 0]
        input.add(insn)
        labels.add(JLabel("Label number: "))
        var count = 0
        if (edit)
            for (insn2 in method.instructions) {
                if (insn2 is LabelNode) {
                    if (insn2 == labelInsnNode?.label) {
                        break
                    }
                    count++
                }
            }
        var label = JTextField(if (count == 0) "" else count.toString())
        input.add(label)
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Insert Jump Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            val labelIndex = Integer.parseInt(label.text)
            var labelNode: LabelNode? = null
            var count2 = 0
            for (insn2 in method.instructions) {
                if (insn2 is LabelNode) {
                    if (count2 == labelIndex) {
                        labelNode = insn2
                        break
                    }
                    count2++
                }
            }
            if (labelNode == null) {
                throw Exception()
            } else {
                Edit.insertOrReplaceInsn(JumpInsnNode(opcode, labelNode), node, method.instructions, edit)
            }
        }
    }

    fun ldcInsnEditor(node: AbstractInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, edit: Boolean) {
        labels.add(JLabel("Type: "))
        val insn = JComboBox(arrayOf("String", "int", "float"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode ?: 0]
        input.add(insn)
        labels.add(JLabel("Value: "))
        val value = JTextField(if (node is LdcInsnNode) node.cst.toString() else "")
        input.add(value)
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Insert LDC Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            when (insn.selectedItem as String) {
                "String" -> Edit.insertOrReplaceInsn(LdcInsnNode(value.text), node, method.instructions, edit)
                "int" -> Edit.insertOrReplaceInsn(LdcInsnNode(Integer.parseInt(value.text)), node, method.instructions, edit)
                "float" -> Edit.insertOrReplaceInsn(LdcInsnNode(java.lang.Float.parseFloat(value.text)), node, method.instructions, edit)
            }
        }
    }

    fun lineNumberEditor(node: AbstractInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, edit: Boolean) {
        val lineNode = if (node is LineNumberNode) node else null
        labels.add(JLabel("Line number: "))
        var value = JTextField(lineNode?.line?.toString() ?: "")
        input.add(value)
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Insert Line Number",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            val label = LabelNode()
            if (!edit || lineNode == null) {
                method.instructions.insert(node, LineNumberNode(Integer.parseInt(value.text), label))
                method.instructions.insert(node, label)
            } else {
                lineNode.line = Integer.parseInt(value.text)
            }
        }
    }

    fun typeInsnEditor(node: AbstractInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, edit: Boolean) {
        labels.add(JLabel("Instruction: "))
        val insn = JComboBox(arrayOf("new", "anewarray", "checkcast", "instanceof"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode ?: 0]
        input.add(insn)
        labels.add(JLabel("Type description: "))
        val desc = JTextField(if (node is TypeInsnNode) node.desc else "")
        input.add(desc)
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Insert Type Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            Edit.insertOrReplaceInsn(TypeInsnNode(opcode, desc.text), node, method.instructions, edit)
        }
    }

    fun varInsnEditor(node: AbstractInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, edit: Boolean) {
        labels.add(JLabel("Type: "))
        val insn = JComboBox(arrayOf("iload", "lload", "fload", "dload",
                "aload", "istore", "lstore", "fstore", "dstore", "astore", "ret"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode ?: 0]
        input.add(insn)
        labels.add(JLabel("Local variable index: "))
        val index = JTextField(if (node is VarInsnNode) node.`var`.toString() else "")
        input.add(index)
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Insert Var Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            Edit.insertOrReplaceInsn(VarInsnNode(opcode, Integer.parseInt(index.text)), node, method.instructions, edit)
        }
    }

    fun fieldInsnEditor(node: AbstractInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, edit: Boolean) {
        val fieldInsnNode = if (node is FieldInsnNode) node else null
        labels.add(JLabel("Type: "))
        val insn = JComboBox(arrayOf("getstatic", "putstatic", "getfield", "putfield"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode ?: 0]
        input.add(insn)
        labels.add(JLabel("Owner: "))
        val owner = JTextField(fieldInsnNode?.owner ?: "")
        input.add(owner)
        labels.add(JLabel("Name: "))
        val name = JTextField(fieldInsnNode?.name ?: "")
        input.add(name)
        labels.add(JLabel("Description: "))
        val desc = JTextField(fieldInsnNode?.desc ?: "")
        input.add(desc)
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Insert Field Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            Edit.insertOrReplaceInsn(FieldInsnNode(opcode, owner.text, name.text, desc.text), node, method.instructions, edit)
        }
    }

    fun methodInsnEditor(node: AbstractInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, edit: Boolean) {
        val methodNode = if (node is MethodInsnNode) node else null
        labels.add(JLabel("Type: "))
        val insn = JComboBox(arrayOf("invokestatic", "invokevirtual", "invokespecial", "invokeinterface"))
        insn.selectedItem = OpUtil.mnemonics[node?.opcode ?: 0]
        input.add(insn)
        labels.add(JLabel("Owner: "))
        val owner = JTextField(methodNode?.owner ?: "")
        input.add(owner)
        labels.add(JLabel("Name: "))
        val name = JTextField(methodNode?.name ?: "")
        input.add(name)
        labels.add(JLabel("Description: "))
        val desc = JTextField(methodNode?.desc ?: "")
        input.add(desc)
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Insert Method Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            Edit.insertOrReplaceInsn(MethodInsnNode(opcode, owner.text, name.text, desc.text), node, method.instructions, edit)
        }
    }

    fun insnEditor(node: AbstractInsnNode?, method: MethodNode, panel: JPanel, labels: JPanel, input: JPanel, edit: Boolean) {
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
        insn.selectedItem = OpUtil.mnemonics[node?.opcode ?: 0]
        input.add(insn)
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Insert Instruction",
                JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            var opcode = OpUtil.opcodes[insn.selectedItem as String]!!
            Edit.insertOrReplaceInsn(InsnNode(opcode), node, method.instructions, edit)
        }
    }

    fun searchString() {

        val panel = JPanel(BorderLayout(5, 5))
        val input = JPanel(GridLayout(0, 1))
        val labels = JPanel(GridLayout(0, 1))
        panel.add(labels, BorderLayout.WEST)
        panel.add(input, BorderLayout.CENTER)
        labels.add(JLabel("String: "))
        val text = JTextField()
        input.add(text)
        val checkbox = JCheckBox("Match case", false)
        panel.add(checkbox, BorderLayout.SOUTH)
        val result = JOptionPane.showConfirmDialog(JBytedit, panel, "Search String",
                JOptionPane.OK_CANCEL_OPTION)
        try {
            if (result == JOptionPane.OK_OPTION) {
                val searchString = if (checkbox.isSelected) text.text else text.text.toLowerCase()
                val results = Vector<SearchEntry>()
                for (classNode in FileUtil.classes.values){
                    for (method in classNode!!.methods){
                        if (method is MethodNode) {
                            var i = 0
                            for (instruction in method.instructions) {
                                if (instruction is LdcInsnNode && instruction.cst is String) {
                                    val str = instruction.cst.toString()
                                    if (str.contains(searchString, !checkbox.isSelected)) {
                                        results.addElement(SearchEntry(
                                                TextUtil.toHtml(OpUtil.getDisplayClass(classNode.name) + "." +
                                                        TextUtil.escapeHTML(method.name) + " - " +
                                                        TextUtil.addTag("\"${str}\"", "font color=#559955")),
                                                classNode, method, instruction))
                                    }
                                }
                                i++
                            }
                        }
                    }
                    for (field in classNode.fields){
                        if (field is FieldNode){
                            if (field.value != null && field.value is String) {
                                val str = field.value.toString()
                                if (str.contains(searchString, !checkbox.isSelected)) {
                                    results.addElement(SearchEntry(TextUtil.toHtml(OpUtil.getDisplayClass(classNode.name) + "." + field.name), classNode, null, null))
                                }
                            }
                        }
                    }
                }
                JBytedit.openResults(Component.resultsList(results))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            JOptionPane.showMessageDialog(JBytedit,
                    "An error occurred",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

}