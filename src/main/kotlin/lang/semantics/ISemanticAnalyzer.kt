package lang.semantics

import lang.compiler.Module
import lang.messages.MsgHandler
import lang.nodes.BlockNode
import lang.semantics.resolvers.ConstResolver
import lang.semantics.resolvers.DeclarationResolver
import lang.semantics.resolvers.TypeResolver
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeError
import lang.semantics.symbols.Symbol
import lang.tokens.Pos

interface ISemanticAnalyzer {
    val scope: Scope
    val msgHandler: MsgHandler

    val declResolver: DeclarationResolver
    val constResolver: ConstResolver
    val typeResolver: TypeResolver

    val semanticContext: SemanticContext

    fun resolve(module: Module)

    fun exportSymbol(sym: Symbol)

    fun exitScope()
    fun enterScope(newScope: Scope)
    fun <T> withScope(targetScope: Scope = Scope(parent = this.scope), block: () -> T) : T

    fun withScopeResolveBody(targetScope: Scope, body: BlockNode?)

    fun scopeError(error: ScopeError, pos: Pos?)
    fun warning(msg: String, pos: Pos)
}