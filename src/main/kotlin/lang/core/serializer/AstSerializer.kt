package lang.core.serializer

import lang.nodes.*
import lang.semantics.SemanticContext
import lang.semantics.symbols.Symbol

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

            is SizeofNode -> mapOf(
                "datatype" to expr.datatype
            )

            is AlignofNode -> mapOf(
                "datatype" to expr.datatype
            )

            is OffsetofNode -> mapOf(
                "base" to expr.base,
                "member" to expr.field
            )

            is UnaryOpNode -> mapOf(
                "operator" to expr.operator,
                "operand" to expr.operand
            )

            is FuncCallNode -> mapOf(
                "name" to expr.receiver,
                "typeNames" to expr.typeArgs,
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
                "dataType" to expr.datatype,
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
                "typeNames" to expr.templates,
                "params" to expr.params,
                "returnType" to expr.returnType,
                "isExpressionBodied" to expr.isExpressionBodied,
                "extensionDatatype" to expr.extensionDatatype,
                "body" to expr.body,
            )

            is InterfaceDeclStmtNode -> mapOf(
                "modifiers" to expr.modifiers,
                "name" to expr.name,
                "typeNames" to expr.templates,
                "superInterface" to expr.superInterface,
                "body" to expr.body,
            )

            is ClassDeclStmtNode -> mapOf(
                "modifiers" to expr.modifiers,
                "name" to expr.name,
                "typeNames" to expr.templates,
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

            is MethodDatatypeNode -> mapOf(
                "ownerDatatype" to expr.ownerDatatype,
                "funcDatatype" to expr.funcDatatype
            )

            is DatatypeNode -> mapOf(
                "name" to expr.identifier,
                "isConst" to expr.isConst,
                "isReference" to expr.isReference,
                "ptrLvl" to expr.ptrLvl,
                "typeNames" to expr.typeArgs
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
                "clause" to expr.clause,
                "modifiers" to expr.modifiers
            )

            is UsingStmtNode -> mapOf(
                "scopedExpr" to expr.scopedExpr,
                "body" to expr.body
            )

            is TemplateParamsListNode -> mapOf(
                "params" to expr.params
            )

            is TemplateParamNode -> mapOf(
                "name" to expr.name,
                "bound" to expr.bound
            )
            else -> emptyMap()
        }

        return map + mapOf("range" to expr.range)
    }
}