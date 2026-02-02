package lang.semantics

import lang.messages.ErrorHandler
import lang.messages.Messages
import lang.nodes.*
import lang.semantics.resolvers.ConstResolver
import lang.semantics.resolvers.DeclarationResolver
import lang.semantics.resolvers.TypeResolver
import lang.semantics.scopes.GlobalScope
import lang.semantics.scopes.Scope
import lang.tokens.Pos

class SemanticAnalyzer(
    override val errorHandler: ErrorHandler
) : ISemanticAnalyzer {
    override var scope: Scope = GlobalScope(errorHandler = errorHandler)

    override val declResolver: DeclarationResolver = DeclarationResolver(analyzer = this)
    override val constResolver: ConstResolver = ConstResolver(analyzer = this)
    override val typeResolver: TypeResolver = TypeResolver(analyzer = this)

    override val semanticContext = SemanticContext()



    init {
//        PrimitiveTypes.initBuiltInTypes(
//            scope = scope,
//            errorHandler = errorHandler
//        )
    }

    override fun resolve(node: ExprNode) {
        when (node) {
            is DeclStmtNode -> declResolver.resolve(node)
            is BlockNode -> resolve(node = node)
            else -> typeResolver.resolve(target = node)
        }
    }

    override fun exitScope() {
        val parent = scope.parent ?: return
        scope = parent
    }

    override fun enterScope(newScope: Scope) {
        scope = newScope
    }

    override fun <T> withScope(
        targetScope: Scope,
        block: () -> T
    ) : T {
        val prev = scope
        scope = targetScope
        try {
            return block()
        } finally {
            scope = prev
        }
    }


    override fun withScopeResolveBody(targetScope: Scope, body: BlockNode?) {
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

    override fun resolve(node: BlockNode) {
        node.nodes.forEach { node -> resolve(node) }
    }

    private fun symNotDefinedError(name: String, pos: Pos) {
        semanticError(Messages.F_SYMBOL_NOT_DEFINED_CUR.format(name), pos)
    }

    private fun IdentifierNode.error(func: (String, Pos) -> Unit) = func(value, pos)

    private fun semanticError(msg: String, pos: Pos) {
        errorHandler.semanticError(msg, pos)
    }
}