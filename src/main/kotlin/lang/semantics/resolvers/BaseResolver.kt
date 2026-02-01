package lang.semantics.resolvers

import lang.messages.ErrorHandler
import lang.messages.Messages
import lang.nodes.ExprNode
import lang.nodes.IdentifierNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.scopes.Scope
import lang.semantics.symbols.Symbol
import lang.semantics.types.ErrorType
import lang.semantics.types.Type
import lang.tokens.Pos
import kotlin.collections.set

abstract class BaseResolver<T, TResult>(
    open val analyzer: ISemanticAnalyzer
) {
    internal val scope: Scope
        get() = analyzer.scope

    internal val errorHandler: ErrorHandler
        get() = analyzer.errorHandler

    abstract fun resolve(target: T): TResult

    internal fun symNotDefinedError(name: String, pos: Pos) =
        semanticError(Messages.F_SYMBOL_NOT_DEFINED_CUR.format(name), pos)

    internal fun symNotDefinedInError(name: String, scopeName: String, pos: Pos) =
        semanticError(Messages.F_SYMBOL_NOT_DEFINED_IN.format(name, scopeName), pos)

    internal fun IdentifierNode.error(func: (String, Pos) -> ErrorType) = func(value, pos)
    internal fun IdentifierNode.error(func: (String, String, Pos) -> ErrorType) =
        func(value, scope.absoluteScopePath ?: "", pos)

    internal fun ExprNode.error(msg: String) = semanticError(msg, pos)

    internal fun symDefinedError(name: String, pos: Pos) =
        semanticError(Messages.F_SYMBOL_ALREADY_DEFINED.format(name), pos)

    internal fun semanticError(msg: String, pos: Pos?): ErrorType {
        analyzer.errorHandler.semanticError(msg, pos)
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