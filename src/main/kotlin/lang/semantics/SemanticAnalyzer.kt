package lang.semantics

import lang.messages.ErrorHandler
import lang.messages.Messages
import lang.nodes.*
import lang.semantics.scopes.FuncParamsScope
import lang.semantics.scopes.FuncScope
import lang.semantics.scopes.GlobalScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.InterfaceSymbol
import lang.tokens.Pos

class SemanticAnalyzer(
    private val errorHandler: ErrorHandler
) : ISemanticAnalyzer {
    private var scope: Scope = GlobalScope()

    init {
        initBuiltInTypes()
    }

    override fun resolve(node: ExprNode) {
        when (node) {
            is DeclStmtNode -> resolve(node)
            is BlockNode -> resolve(node = node)
            is BaseDatatypeNode -> resolve(node = node)
//            is LambdaNode -> resolve(node = node.body)
        }
    }

    private fun initBuiltInTypes() {
        arrayOf(
            "float", "double", "long", "short", "char", "byte",
            "int", "int8", "int16", "int32", "int64",
            "uint", "uint8", "uint16", "uint32", "uint64"
        ).forEach {
            scope.define(sym = InterfaceSymbol(name = it))
        }
    }

    fun exitScope() {
        val parent = scope.parent ?: return
        scope = parent
    }

    fun enterScope(newScope: Scope) {
        scope = newScope
    }

    private fun withScope(scope: Scope = Scope(parent = this.scope), block: () -> Unit) {
        enterScope(newScope = scope)
        block.invoke()
        exitScope()
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

            is FuncDatatypeNode -> {}
            is VoidDatatypeNode -> {}
            is AutoDatatypeNode -> {}
            is ErrorDatatypeNode -> {}
        }
    }

    private fun resolve(node: DeclStmtNode) {
        when (node) {
            is VarDeclStmtNode -> resolve(node)
            is FuncDeclStmtNode -> resolve(node)
            is ConstructorDeclStmtNode -> resolve(node)
            is DestructorDeclStmtNode -> resolve(node)
            is InterfaceDeclStmtNode -> resolve(node)
            is ClassDeclStmtNode -> resolve(node)
            is EnumDeclStmtNode -> resolve(node)
        }
    }

    private fun resolve(node: VarDeclStmtNode) {
        val symbol = scope.defineVar(node)
        node.apply {
            this.symbol = symbol

            if (symbol == null)
                symDefinedError(name = name.value, pos = pos)

            when (dataType) {
                is AutoDatatypeNode -> {
                    if (initializer == null) {
                        semanticError(Messages.EXPECTED_TYPE_NAME, name.pos)
                    }else {
                        // get datatype of initializer
                    }
                }

                else -> resolve(dataType)
            }
        }
    }

    private fun resolveFuncParam(node: VarDeclStmtNode) {
        val paramsScope = scope

        if (paramsScope !is FuncParamsScope) {
            semanticError(Messages.CANNOT_RESOLVE_PARAM_OUTSIDE_FUNC, node.pos)
            return
        }

        paramsScope.defineParam(node)
    }

    private fun resolve(node: FuncDeclStmtNode) {
        val funcScope = FuncParamsScope(parent = scope)
        enterScope(funcScope)

        node.params.forEach { decl ->
            resolveFuncParam(node = decl)
        }

        val params = funcScope.getParams()
        exitScope()

//        scope.defineFunc()
    }

    private fun resolve(node: ConstructorDeclStmtNode) {

    }

    private fun resolve(node: DestructorDeclStmtNode) {

    }

    private fun resolve(node: InterfaceDeclStmtNode) {

    }

    private fun resolve(node: ClassDeclStmtNode) {

    }

    private fun resolve(node: EnumDeclStmtNode) {

    }

    override fun resolve(node: BlockNode) {
        withScope {
            node.nodes.forEach { node -> resolve(node) }
        }
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
}