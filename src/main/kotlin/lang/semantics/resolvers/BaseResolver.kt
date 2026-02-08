package lang.semantics.resolvers

import lang.core.SourceRange
import lang.messages.Msg
import lang.messages.MsgHandler
import lang.nodes.ExprNode
import lang.nodes.IdentifierNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.scopes.ModuleScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.Symbol
import lang.semantics.types.ErrorType
import lang.semantics.types.Type

abstract class BaseResolver<T, TResult>(
    open val analyzer: ISemanticAnalyzer
) {
    internal val scope: Scope
        get() = analyzer.scope

    internal val msgHandler: MsgHandler
        get() = analyzer.msgHandler

    abstract fun resolve(target: T): TResult

    internal fun symNotDefinedError(name: String, range: SourceRange) =
        semanticError(Msg.SymbolNotDefinedIn.format(name = name, scopeName = scope.scopeName), range)

    internal fun symNotDefinedInError(name: String, scopeName: String?, range: SourceRange): ErrorType {
        return when {
            scopeName.isNullOrEmpty() ->
                symNotDefinedError(name, range)

            scope is ModuleScope ->
                semanticError(
                    Msg.F_MODULE_DOES_NOT_EXPORT_SYM.format(
                        name,
                        scopeName
                    ), range
                )

            else ->
                semanticError(
                    Msg.SymbolNotDefinedIn.format(
                        name = name,
                        scopeName = scopeName
                    ), range
                )
        }
    }

    fun ExprNode.error(msg: String) = semanticError(msg, range)

    internal fun semanticError(msg: String, range: SourceRange?): ErrorType {
        analyzer.msgHandler.semanticError(msg, range)
        return ErrorType
    }

    infix fun ExprNode.attach(type: Type?) {
        if (type == null) return
        analyzer.semanticContext.types[this] = type
    }

    infix fun ExprNode.bind(symbol: Symbol?) {
        if (symbol == null) return
        analyzer.semanticContext.symbols[this] = symbol
    }

    fun ExprNode.getResolvedSymbol(): Symbol? =
        analyzer.semanticContext.symbols[this]

    fun ExprNode.getResolvedType(): Type? =
        analyzer.semanticContext.types[this]
}