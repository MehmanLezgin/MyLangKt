package lang.semantics

import lang.compiler.Module
import lang.messages.ErrorHandler
import lang.nodes.BlockNode
import lang.semantics.resolvers.ConstResolver
import lang.semantics.resolvers.DeclarationResolver
import lang.semantics.resolvers.TypeResolver
import lang.semantics.scopes.Scope
import lang.semantics.symbols.Symbol

interface ISemanticAnalyzer {
    val scope: Scope
    val errorHandler: ErrorHandler

    val declResolver: DeclarationResolver
    val constResolver: ConstResolver
    val typeResolver: TypeResolver

    val semanticContext: SemanticContext

    fun resolve(module: Module)

    fun exportSymbol(sym: Symbol)

    fun exitScope()
    fun enterScope(newScope: Scope)
    fun <T> withScope(targetScope: Scope = Scope(parent = this.scope, errorHandler), block: () -> T) : T

    fun withScopeResolveBody(targetScope: Scope, body: BlockNode?)
}