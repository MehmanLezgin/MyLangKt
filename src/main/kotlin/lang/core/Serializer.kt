package lang.core

import lang.nodes.BinOpNode
import lang.nodes.BlockNode
import lang.nodes.ClassDeclStmtNode
import lang.nodes.ConstructorDeclStmtNode
import lang.nodes.DatatypeNode
import lang.nodes.DecrementNode
import lang.nodes.DestructorDeclStmtNode
import lang.nodes.DoWhileStmtNode
import lang.nodes.ElseEntryNode
import lang.nodes.EnumItemNode
import lang.nodes.EnumDeclStmtNode
import lang.nodes.ExprNode
import lang.nodes.ForLoopStmtNode
import lang.nodes.FuncCallNode
import lang.nodes.FuncDatatypeNode
import lang.nodes.FuncDeclStmtNode
import lang.nodes.IdentifierNode
import lang.nodes.IfElseStmtNode
import lang.nodes.ImportStmtNode
import lang.nodes.IncrementNode
import lang.nodes.IndexAccessNode
import lang.nodes.InterfaceDeclStmtNode
import lang.nodes.LambdaNode
import lang.nodes.LiteralNode
import lang.nodes.MatchStmtNode
import lang.nodes.MemberAccessNode
import lang.nodes.NamespaceStmtNode
import lang.nodes.NullLiteralNode
import lang.nodes.OperNode
import lang.nodes.ReturnStmtNode
import lang.nodes.TryCatchStmtNode
import lang.nodes.TypedefStmtNode
import lang.nodes.UnaryOpNode
import lang.nodes.UnknownNode
import lang.nodes.VarDeclStmtNode
import lang.nodes.WhileStmtNode

object Serializer {
    fun formatNode(
        node: ExprNode,
        indent: String = "",
        isLast: Boolean = true
    ): String {
        val branch = if (isLast) "└── " else "├── "
        val nextIndent = indent + if (isLast) "    " else "│   "

        val header = "${getName(node)} pos=${node.pos}"
        val children = getChildren(node)

        if (children.isEmpty()) {
            return indent + branch + header
        }

        val builder = StringBuilder(indent + branch + header)

        for ((i, child) in children.withIndex()) {
            val (label, expr) = child
            val last = i == children.lastIndex

            builder.append("\n")
            builder.append(nextIndent)
            builder.append(if (last) "└── " else "├── ")
            builder.append(label)


            if (expr is ExprNode) {
                builder.append(":\n")
                builder.append(formatNode(expr, nextIndent + if (last) "    " else "│   ", true))
                continue
            } else {
                builder.append(": ")
                val str = if (expr is String) "\"$expr\"" else expr
                builder.append(str)
                builder.append('\n')
            }

            if (expr is List<*>) {
                builder.append(":\n")
                for ((j, elem) in expr.withIndex()) {
                    val isLastElem = j == expr.lastIndex
                    val prefix = nextIndent + if (isLast) "    " else "│   "

                    when (elem) {
                        is Pair<*, *> -> {
                            builder.append(prefix)
                            builder.append(if (isLastElem) "└── " else "├── ")
                            builder.append("${elem.first}:\n")

                            val value = elem.second
                            if (value is ExprNode)
                                builder.append(
                                    formatNode(
                                        value,
                                        prefix + if (isLastElem) "    " else "│   ",
                                        true
                                    )
                                )
                            else {
                                builder.append("$prefix    ")
                                builder.append(value.toString())
                            }

                        }

                        is ExprNode -> {
                            builder.append(prefix)
                            builder.append(if (isLastElem) "└── " else "├── ")
                            builder.append(formatNode(elem, prefix, true))
                            builder.append("\n")
                        }

                        else -> {
                            builder.append(prefix)
                            builder.append(if (isLastElem) "└── " else "├── ")
                            builder.append(elem.toString())
                            builder.append("\n")
                        }
                    }
                }
                continue
            }

        }

        return builder.toString().replace("\n\n", "\n")
    }

    private fun getChildren(node: ExprNode): List<Pair<String, Any?>> {
        return when (node) {
            is UnknownNode -> emptyList()

            is OperNode -> listOf(
                "type" to node.type,
                "value" to node.value
            )

            is IdentifierNode -> listOf(
                "value" to node.value
            )

            is LiteralNode<*> -> listOf(
                "value" to node.value,
                "type" to node.value!!::class.simpleName.toString()
            )

            is NullLiteralNode -> emptyList()

            is BlockNode -> node.nodes.mapIndexed { i, expr -> "[$i]" to expr }

            is BinOpNode -> listOf(
                "operator" to node.operator,
                "left" to node.left,
                "right" to node.right
            )

            is IncrementNode -> listOf(
                "isPost" to node.isPost,
                "operand" to node.operand
            )

            is DecrementNode -> listOf(
                "isPost" to node.isPost,
                "operand" to node.operand
            )

            is UnaryOpNode -> listOf(
                "operator" to node.operator,
                "operand" to node.operand
            )

            is FuncCallNode -> listOf(
                "name" to node.name,
                "typeNames" to node.typeNames,
                "args" to node.args
            )

            is IndexAccessNode -> listOf(
                "name" to node.target,
                "index" to node.indexExpr
            )

            // Statements
            is IfElseStmtNode -> listOfNotNull(
                "condition" to node.condition,
                "body" to node.body,
                node.elseBody?.let { "elseBody" to it }
            )

            is WhileStmtNode -> listOfNotNull(
                "condition" to node.condition,
                "body" to node.body,
                node.elseBody?.let { "elseBody" to it }
            )

            is DoWhileStmtNode -> listOf(
                "condition" to node.condition,
                "body" to node.body
            )

            is ForLoopStmtNode -> listOf(
                "condition" to node.condition,
                "body" to node.body
            )

            is VarDeclStmtNode -> listOf(
                "modifiers" to node.modifiers,
                "isMutable" to node.isMutable,
                "name" to node.name,
                "dataType" to node.dataType,
                "initializer" to node.initializer
            )

            is FuncDeclStmtNode -> listOf(
                "modifiers" to node.modifiers,
                "name" to node.name,
                "typeNames" to node.typeNames,
                "params" to node.params,
                "returnType" to node.returnType,
                "body" to node.body,
            )

            is ConstructorDeclStmtNode -> listOf(
                "modifiers" to node.modifiers,
                "params" to node.params,
                "body" to node.body,
            )

            is DestructorDeclStmtNode -> listOf(
                "modifiers" to node.modifiers,
                "body" to node.body
            )

            is InterfaceDeclStmtNode -> listOf(
                "modifiers" to node.modifiers,
                "name" to node.name,
                "typeNames" to node.typeNames,
                "superInterface" to node.superInterface,
                "body" to node.body,
                "pos" to node.pos
            )

            is ClassDeclStmtNode -> listOf(
                "modifiers" to node.modifiers,
                "name" to node.name,
                "typeNames" to node.typeNames,
                "primaryConstrParams" to node.primaryConstrParams,
                "superClass" to node.superClass,
                "body" to node.body,
                "pos" to node.pos
            )

            is ReturnStmtNode -> listOf(
                "expr" to node.expr
            )

            is FuncDatatypeNode -> listOf(
                "isConst" to node.isConst,
                "isReference" to node.isReference,
                "ptrLvl" to node.ptrLvl,
                "paramDatatypes" to node.paramDatatypes,
                "returnDatatype" to node.returnDatatype
            )

            is DatatypeNode -> listOf(
                "name" to node.identifier,
                "isConst" to node.isConst,
                "isReference" to node.isReference,
                "ptrLvl" to node.ptrLvl,
                "typeNames" to node.typeNames
            )

            is EnumDeclStmtNode -> listOf(
                "name" to node.name,
                "items" to node.body
            )

            is EnumItemNode -> listOf(
                "name" to node.name,
                "initializer" to node.initializer
            )

            is MatchStmtNode -> listOf(
                "target" to node.target,
                "body" to node.body
            )

            is ElseEntryNode -> listOf(
                "expr" to node.expr
            )

            is TryCatchStmtNode -> listOf(
                "tryBody" to node.tryBody,
                "catchParam" to node.catchParam,
                "catchBody" to node.catchBody,
                "finallyBody" to node.finallyBody,
            )
            is ImportStmtNode -> listOf(
                "path" to node.path
            )
            is NamespaceStmtNode -> listOf(
                "name" to node.name,
                "body" to node.body
            )

            is LambdaNode -> listOf(
                "body" to node.body,
                "params" to node.params
            )

            is TypedefStmtNode -> listOf(
                "identifier" to node.identifier,
                "dataType" to node.dataType
            )

            is MemberAccessNode -> listOf(
                "base" to node.base,
                "member" to node.member,
                "isNullSafe" to node.isNullSafe
            )

            else -> emptyList()
        }
    }

    private fun listChildren(list: List<*>): List<Pair<String, Any?>> {
        return list.mapIndexed { i, expr -> "[$i]" to expr }
    }

    fun getName(node: ExprNode) = node.javaClass.simpleName
}