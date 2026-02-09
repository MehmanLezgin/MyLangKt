package lang.core.serializer

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
import lang.nodes.IncrementNode
import lang.nodes.IndexAccessNode
import lang.nodes.InterfaceDeclStmtNode
import lang.nodes.LambdaNode
import lang.nodes.LiteralNode
import lang.nodes.MatchStmtNode
import lang.nodes.DotAccessNode
import lang.nodes.ImportFromStmtNode
import lang.nodes.ImportModulesStmtNode
import lang.nodes.ModifierSetNode
import lang.nodes.ModuleStmtNode
import lang.nodes.NameClause
import lang.nodes.NameSpecifier
import lang.nodes.NullLiteralNode
import lang.nodes.OperNode
import lang.nodes.ReturnStmtNode
import lang.nodes.ScopedDatatypeNode
import lang.nodes.TryCatchStmtNode
import lang.nodes.UnaryOpNode
import lang.nodes.UnknownNode
import lang.nodes.UsingDirectiveNode
import lang.nodes.UsingStmtNode
import lang.nodes.VarDeclStmtNode
import lang.nodes.WhileStmtNode
import lang.semantics.SemanticContext
import lang.semantics.symbols.Symbol
import kotlin.collections.toMutableMap

object AstSerializer {
    fun serialize(
        root: ExprNode,
        semanticContext: SemanticContext?
    ): String {
        return serialize(root, ExprNode::class) { expr, _, nextIndent ->
            val children = getNodeChildren(expr).toMutableMap()
            semanticContext?.types[expr]?.let {
                children["type"] = it
            }

            semanticContext?.symbols[expr]?.let {
                children["symbol"] = it
            }


            children.mapWithSymbols(nextIndent)
        }
    }

    fun ChildrenMapRaw.mapWithSymbols(currIndent: String): ChildrenMapRaw {
        return this.toMap().map {
            when (val value = it.value) {
                is Symbol ->
                    it.key to SymbolSerializer.serialize(value, currIndent)

                else -> it.key to it.value
            }
        }.toMap()
    }

    private fun getNodeChildren(expr: ExprNode): ChildrenMapRaw {
        val map = when (expr) {
            is OperNode -> mapOf(
                "type" to expr.operatorType,
                "value" to expr.value
            )

            is IdentifierNode -> mapOf(
                "value" to expr.value
            )

            is LiteralNode<*> -> mapOf(
                "value" to expr.value,
                "type" to expr.value::class.simpleName.toString()
            )

            is ModuleStmtNode -> mapOf(
                "name" to expr.name,
                "body" to expr.body
            )

            is BlockNode -> expr.nodes
                .mapIndexed { i, expr -> "[$i]" to expr }
                .toMap()

            is BinOpNode -> mapOf(
                "operator" to expr.operator,
                "left" to expr.left,
                "right" to expr.right
            )

            is IncrementNode -> mapOf(
                "isPost" to expr.isPost,
                "operand" to expr.operand
            )

            is DecrementNode -> mapOf(
                "isPost" to expr.isPost,
                "operand" to expr.operand
            )

            is UnaryOpNode -> mapOf(
                "operator" to expr.operator,
                "operand" to expr.operand
            )

            is FuncCallNode -> mapOf(
                "name" to expr.receiver,
                "typeNames" to expr.typeNames,
                "args" to expr.args
            )

            is IndexAccessNode -> mapOf(
                "name" to expr.target,
                "index" to expr.indexExpr
            )

            // Statements
            is IfElseStmtNode -> mapOf(
                "condition" to expr.condition,
                "body" to expr.body,
                "elseBody" to expr.elseBody
            )

            is WhileStmtNode -> mapOf(
                "condition" to expr.condition,
                "body" to expr.body,
                "elseBody" to expr.elseBody
            )

            is DoWhileStmtNode -> mapOf(
                "condition" to expr.condition,
                "body" to expr.body
            )

            is ForLoopStmtNode -> mapOf(
                "condition" to expr.condition,
                "body" to expr.body
            )

            is VarDeclStmtNode -> mapOf(
                "modifiers" to expr.modifiers,
                "isMutable" to expr.isMutable,
                "name" to expr.name,
                "dataType" to expr.dataType,
                "initializer" to expr.initializer
            )

            is ConstructorDeclStmtNode -> mapOf(
                "modifiers" to expr.modifiers,
                "params" to expr.params,
                "body" to expr.body,
            )

            is DestructorDeclStmtNode -> mapOf(
                "modifiers" to expr.modifiers,
                "body" to expr.body
            )

            is FuncDeclStmtNode -> mapOf(
                "modifiers" to expr.modifiers,
                "name" to expr.name,
                "typeNames" to expr.typeNames,
                "params" to expr.params,
                "returnType" to expr.returnType,
                "body" to expr.body,
            )

            is InterfaceDeclStmtNode -> mapOf(
                "modifiers" to expr.modifiers,
                "name" to expr.name,
                "typeNames" to expr.typeNames,
                "superInterface" to expr.superInterface,
                "body" to expr.body,
            )

            is ClassDeclStmtNode -> mapOf(
                "modifiers" to expr.modifiers,
                "name" to expr.name,
                "typeNames" to expr.typeNames,
                "primaryConstrParams" to expr.primaryConstrParams,
                "superClass" to expr.superClass,
                "body" to expr.body,
            )

            is ReturnStmtNode -> mapOf(
                "expr" to expr.expr
            )

            is FuncDatatypeNode -> mapOf(
                "isConst" to expr.isConst,
                "isReference" to expr.isReference,
                "ptrLvl" to expr.ptrLvl,
                "paramDatatypes" to expr.paramDatatypes,
                "returnDatatype" to expr.returnDatatype
            )

            is DatatypeNode -> mapOf(
                "name" to expr.identifier,
                "isConst" to expr.isConst,
                "isReference" to expr.isReference,
                "ptrLvl" to expr.ptrLvl,
                "typeNames" to expr.typeNames
            )

            is ScopedDatatypeNode -> mapOf(
                "base" to expr.base,
                "member" to expr.member
            )

            is EnumDeclStmtNode -> mapOf(
                "name" to expr.name,
                "items" to expr.body
            )

            is EnumItemNode -> mapOf(
                "name" to expr.name,
                "initializer" to expr.initializer
            )

            is MatchStmtNode -> mapOf(
                "target" to expr.target,
                "body" to expr.body
            )

            is ElseEntryNode -> mapOf(
                "expr" to expr.expr
            )

            is TryCatchStmtNode -> mapOf(
                "tryBody" to expr.tryBody,
                "catchParam" to expr.catchParam,
                "catchBody" to expr.catchBody,
                "finallyBody" to expr.finallyBody,
            )

            is ImportFromStmtNode -> mapOf(
                "moduleName" to expr.sourceName,
                "items" to expr.items
            )

            is ImportModulesStmtNode -> mapOf(
                "items" to expr.items,
            )

            is LambdaNode -> mapOf(
                "body" to expr.body,
                "params" to expr.params
            )

            is DotAccessNode -> mapOf(
                "base" to expr.base,
                "member" to expr.member
            )

            is ModifierSetNode -> mapOf(
                "nodes" to expr.nodes.toList()
            )

            is UsingDirectiveNode -> mapOf(
                "name" to expr.name,
                "value" to expr.value
            )

            is UsingStmtNode -> mapOf(
                "scopedExpr" to expr.scopedExpr,
                "body" to expr.body
            )

            else -> emptyMap()
        }

        return map + mapOf("range" to expr.range)
    }
}