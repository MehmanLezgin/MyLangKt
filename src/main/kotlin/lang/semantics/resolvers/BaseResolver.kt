package lang.semantics.resolvers

import lang.infrastructure.SourceRange
import lang.messages.MsgHandler
import lang.nodes.ExprNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeResult
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

    fun <T> ScopeResult.handle(range: SourceRange?, onSuccess: ScopeResult.Success<*>.() -> T?): T? {
        return when (this) {
            is ScopeResult.Error -> {
                if (range != null)
                    analyzer.scopeError(error, range)

                null
            }

            is ScopeResult.Success<*> -> {
                onSuccess()
            }

            is ScopeResult.ResultList -> {
                list.forEach { result ->
                    result.handle(range, onSuccess)
                }
                null
            }
        }
    }

    fun <T> withEffectiveScope(isStatic: Boolean, block: () -> T): T {
        return if (!isStatic && scope.isTypeScope())
            analyzer.withScope((scope as BaseTypeScope).instanceScope, block)
        else
            block()
    }
}