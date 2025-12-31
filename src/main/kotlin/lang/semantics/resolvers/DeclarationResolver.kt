package lang.semantics.resolvers

import lang.messages.Messages
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.scopes.ClassScope
import lang.semantics.scopes.FuncParamsScope
import lang.semantics.scopes.FuncScope
import lang.semantics.symbols.ConstValueSymbol
import lang.semantics.types.ErrorType
import lang.semantics.types.OverloadedFuncType
import lang.semantics.types.attachType

class DeclarationResolver(
    override val ctx: ISemanticAnalyzer
) : BaseResolver<DeclStmtNode, Unit>(ctx = ctx) {
    override fun resolve(target: DeclStmtNode) {
        when (target) {
            is VarDeclStmtNode -> resolve(target)
            is ConstructorDeclStmtNode -> resolve(target)
            is DestructorDeclStmtNode -> resolve(target)
            is FuncDeclStmtNode -> resolve(target)
            is InterfaceDeclStmtNode -> resolve(target)
            is ClassDeclStmtNode -> resolve(target)
            is EnumDeclStmtNode -> resolve(target)
        }
    }


    private fun resolve(node: VarDeclStmtNode) {
        val isConst = node.modifiers?.nodes?.any { it is ModifierNode.Const } == true

        val initializer = node.initializer
        val dataType = node.dataType

        val type = when {
            dataType is AutoDatatypeNode -> {
                if (initializer == null) {
                    semanticError(Messages.EXPECTED_TYPE_NAME, node.name.pos)
                    ErrorType
                } else {
                    var type = ctx.typeResolver.resolve(node.initializer)
                    if (node.initializer.symbol is ConstValueSymbol) {
                        type = type.setFlags(isConst = false)
                    } else if (type is OverloadedFuncType) {
                        node.initializer.error(Messages.AMBIGUOUS_OVERLOADED_FUNCTION)
                        type = ErrorType
                    }

                    type
                }
            }

            else -> {
                val type = ctx.typeResolver.resolve(node.dataType)

                if (initializer != null) {
                    ctx.typeResolver.resolveForType(
                        target = initializer,
                        type = type
                    )
                }

                type
            }
        }

        type.attachType(node)

        if (isConst) {
            val constValue = if (!type.isConst) {
                semanticError(Messages.EXPECTED_CONST_VALUE, node.pos)
                null
            } else
                ctx.constResolver.resolve(node.initializer)

            scope.defineConstVar(node, type, constValue)
        } else
            scope.defineVar(node, type)
    }

    private fun resolveFuncParam(node: VarDeclStmtNode) {
        val paramsScope = scope

        if (paramsScope !is FuncParamsScope) {
            semanticError(Messages.CANNOT_RESOLVE_PARAM_OUTSIDE_FUNC, node.pos)
            return
        }

        val type = ctx.typeResolver.resolve(node.dataType)

        paramsScope.defineParam(node, type)

        if (node.dataType is AutoDatatypeNode)
            semanticError(Messages.EXPECTED_TYPE_NAME, node.name.pos)

        ctx.resolve(node.dataType)
    }

    private fun resolve(node: FuncDeclStmtNode) {
        val paramsScope = FuncParamsScope(
            parent = scope,
            errorHandler = errorHandler
        )

        ctx.enterScope(paramsScope)
        node.params.forEach { decl ->
            resolveFuncParam(node = decl)
        }

        val params = paramsScope.getParams()
        ctx.exitScope()

        val returnType = ctx.typeResolver.resolve(node.returnType)

        val funcSymbol = scope.defineFunc(node, params, returnType)
        ctx.resolve(node.returnType)

        val funcScope = FuncScope(
            parent = scope,
            funcSymbol = funcSymbol,
            errorHandler = errorHandler
        )

        ctx.withScopeResolveBody(targetScope = funcScope, body = node.body)
    }

    private fun resolve(node: ConstructorDeclStmtNode) {
        if (scope !is ClassScope)
            semanticError(Messages.CONSTRUCTOR_OUTSIDE_CLASS_ERROR, node.pos)

        resolve(node as FuncDeclStmtNode)
    }

    private fun resolve(node: DestructorDeclStmtNode) {
        if (scope !is ClassScope)
            semanticError(Messages.DESTRUCTOR_OUTSIDE_CLASS_ERROR, node.pos)

        resolve(node as FuncDeclStmtNode)
    }

    private fun resolve(node: InterfaceDeclStmtNode) {
        val sym = scope.defineInterface(node)
        ctx.withScopeResolveBody(targetScope = sym.scope, body = node.body)
    }

    private fun resolve(node: ClassDeclStmtNode) {
        val sym = scope.defineClass(node)
        ctx.withScopeResolveBody(targetScope = sym.scope, body = node.body)
    }

    private fun resolve(node: EnumDeclStmtNode) {
        val sym = scope.defineEnum(node)
        ctx.withScopeResolveBody(targetScope = sym.scope, body = node.body)
    }
}