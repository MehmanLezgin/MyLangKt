package lang.semantics

import lang.compiler.Module
import lang.core.SourceRange
import lang.messages.MsgHandler
import lang.nodes.BlockNode
import lang.semantics.resolvers.ConstResolver
import lang.semantics.resolvers.DeclarationResolver
import lang.semantics.resolvers.ModifierResolver
import lang.semantics.resolvers.TypeResolver
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeError
import lang.semantics.symbols.Symbol

interface ISemanticAnalyzer {
    val scope: Scope
    val msgHandler: MsgHandler

    val declResolver: DeclarationResolver
    val constResolver: ConstResolver
    val typeResolver: TypeResolver
    val modResolver: ModifierResolver

    val semanticContext: SemanticContext

    fun resolve(module: Module)

    fun exportSymbol(sym: Symbol)

    fun exitScope()
    fun enterScope(newScope: Scope)
    fun <T> withScope(targetScope: Scope = Scope(parent = this.scope), block: () -> T) : T

    fun withScopeResolveBody(targetScope: Scope, body: BlockNode?)

    fun scopeError(error: ScopeError, range: SourceRange?)
    fun warning(msg: String, range: SourceRange)
}