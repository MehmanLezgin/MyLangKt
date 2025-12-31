package lang.semantics.resolvers

import lang.messages.ErrorHandler
import lang.messages.Messages
import lang.nodes.ExprNode
import lang.nodes.IdentifierNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.scopes.Scope
import lang.tokens.Pos

abstract class BaseResolver<T, TResult>(
    open val ctx: ISemanticAnalyzer
) {
    internal val scope: Scope
        get() = ctx.scope

    internal val errorHandler: ErrorHandler
        get() = ctx.errorHandler

    abstract fun resolve(target: T): TResult

    internal fun symNotDefinedError(name: String, pos: Pos) {
        semanticError(Messages.SYMBOL_NOT_DEFINED.format(name), pos)
    }

    internal fun IdentifierNode.error(func: (String, Pos) -> Unit) = func(value, pos)
    internal fun ExprNode.error(msg: String) = semanticError(msg, pos)

    internal fun symDefinedError(name: String, pos: Pos) {
        semanticError(Messages.SYMBOL_ALREADY_DEFINED.format(name), pos)
    }

    internal fun semanticError(msg: String, pos: Pos) {
        ctx.errorHandler.semanticError(msg, pos)
    }
}