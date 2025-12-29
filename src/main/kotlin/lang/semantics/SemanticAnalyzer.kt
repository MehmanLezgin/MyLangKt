package lang.semantics

import lang.messages.ErrorHandler
import lang.messages.Messages
import lang.nodes.*
import lang.semantics.scopes.*
import lang.semantics.types.ConstValue
import lang.tokens.Pos

class SemanticAnalyzer(
    private val errorHandler: ErrorHandler
) : ISemanticAnalyzer {
    private var scope: Scope = GlobalScope(errorHandler = errorHandler)

    init {
        BuiltInTypes.initBuiltInTypes(
            scope = scope,
            errorHandler = errorHandler
        )
    }

    override fun resolve(node: ExprNode) {
        when (node) {
            is DeclStmtNode -> resolve(node)
            is BlockNode -> resolve(node = node)
            is BaseDatatypeNode -> resolve(node = node)
            is TypedefStmtNode -> resolve(node = node)
//            is LambdaNode -> resolve(node = node.body)
        }
    }

    private fun resolve(node: DeclStmtNode) {
        when (node) {
            is VarDeclStmtNode -> resolve(node)
            is ConstructorDeclStmtNode -> resolve(node)
            is DestructorDeclStmtNode -> resolve(node)
            is FuncDeclStmtNode -> resolve(node)
            is InterfaceDeclStmtNode -> resolve(node)
            is ClassDeclStmtNode -> resolve(node)
            is EnumDeclStmtNode -> resolve(node)
        }
    }

    private fun resolve(node: TypedefStmtNode) {
        scope.defineTypedef(node)
    }

    fun exitScope() {
        val parent = scope.parent ?: return
        scope = parent
    }

    fun enterScope(newScope: Scope) {
        scope = newScope
    }

    private fun withScope(targetScope: Scope = Scope(parent = this.scope, errorHandler), block: () -> Unit) {
        enterScope(newScope = targetScope)
        block.invoke()
        exitScope()
    }

    private fun withScopeResolveBody(targetScope: Scope, body: BlockNode?) {
        if (body == null) return
        withScope(targetScope = targetScope) {
            resolve(node = body)
        }
    }

    private fun resolve(node: BaseDatatypeNode) {
        when (node) {
            is DatatypeNode -> {
                val name = node.identifier.value
                val sym = scope.resolve(name)

                if (sym == null) {
                    node.identifier.error(::symNotDefinedError)
                    return
                }
            }

            is FuncDatatypeNode -> {
                node.paramDatatypes.forEach { resolve(it) }
                resolve(node.returnDatatype)
            }

            is VoidDatatypeNode -> {}
            is AutoDatatypeNode -> {}
            is ErrorDatatypeNode -> {}
        }
    }

    private fun resolve(node: VarDeclStmtNode) {
        val isConst = node.modifiers?.nodes?.any { it is ModifierNode.Const } == true

        if (isConst)
            scope.defineConstVar(node)
        else
            scope.defineVar(node)

        when (node.dataType) {
            is AutoDatatypeNode -> {
                if (node.initializer == null) {
                    semanticError(Messages.EXPECTED_TYPE_NAME, node.name.pos)
                } else {
                    // get datatype of initializer
                    val constValue = calcConstExpr(node.initializer)
                    println("Const value of '${node.name.value}' = ${constValue?.value}")
                }
            }

            else -> resolve(node.dataType)
        }
    }

    private fun resolveFuncParam(node: VarDeclStmtNode) {
        val paramsScope = scope

        if (paramsScope !is FuncParamsScope) {
            semanticError(Messages.CANNOT_RESOLVE_PARAM_OUTSIDE_FUNC, node.pos)
            return
        }

        paramsScope.defineParam(node) ?: node.name.error(::symDefinedError)

        if (node.dataType is AutoDatatypeNode)
            semanticError(Messages.EXPECTED_TYPE_NAME, node.name.pos)

        resolve(node.dataType)
    }

    private fun resolve(node: FuncDeclStmtNode) {
        val paramsScope = FuncParamsScope(
            parent = scope,
            errorHandler = errorHandler
        )

        enterScope(paramsScope)
        node.params.forEach { decl ->
            resolveFuncParam(node = decl)
        }

        val params = paramsScope.getParams()
        exitScope()

        val funcSymbol = scope.defineFunc(node, params)
        resolve(node.returnType)

        val funcScope = FuncScope(
            parent = scope,
            funcSymbol = funcSymbol,
            errorHandler = errorHandler
        )

        withScopeResolveBody(targetScope = funcScope, body = node.body)
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
        withScopeResolveBody(targetScope = sym.scope, body = node.body)
    }

    private fun resolve(node: ClassDeclStmtNode) {
        val sym = scope.defineClass(node)
        withScopeResolveBody(targetScope = sym.scope, body = node.body)
    }

    private fun resolve(node: EnumDeclStmtNode) {
        val sym = scope.defineEnum(node)
        withScopeResolveBody(targetScope = sym.scope, body = node.body)
    }

    override fun resolve(node: BlockNode) {
        node.nodes.forEach { node -> resolve(node) }
    }

    private fun symNotDefinedError(name: String, pos: Pos) {
        semanticError(Messages.SYMBOL_NOT_DEFINED.format(name), pos)
    }

    private fun IdentifierNode.error(func: (String, Pos) -> Unit) = func(value, pos)

    private fun symDefinedError(name: String, pos: Pos) {
        semanticError(Messages.SYMBOL_ALREADY_DEFINED.format(name), pos)
    }

    private fun semanticError(msg: String, pos: Pos) {
        errorHandler.semanticError(msg, pos)
    }

//    private fun getExprDatatype(expr: ExprNode) : BaseDatatypeNode {
//        when (expr) {
//            is LiteralNode<*> ->...
//        }
//    }

//    private fun getExprDatatype(expr: LiteralNode<*>) : BaseDatatypeNode {
//        when (expr.value) {
//            is Int -> DatatypeNode(...)
//        }
//    }

    private fun calcConstExpr(expr: ExprNode): ConstValue<*>? {
        return ConstResolver.resolve(expr, scope)
    }
}