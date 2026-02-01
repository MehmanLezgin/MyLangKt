package lang.semantics

import lang.messages.ErrorHandler
import lang.nodes.BlockNode
import lang.nodes.ExprNode
import lang.semantics.resolvers.ConstResolver
import lang.semantics.resolvers.DeclarationResolver
import lang.semantics.resolvers.TypeResolver
import lang.semantics.scopes.Scope

interface ISemanticAnalyzer {
    val scope: Scope
    val errorHandler: ErrorHandler

    val declResolver: DeclarationResolver
    val constResolver: ConstResolver
    val typeResolver: TypeResolver

    val semanticContext: SemanticContext

    fun resolve(node: ExprNode)
    fun resolve(node: BlockNode)

    fun exitScope()
    fun enterScope(newScope: Scope)
    fun <T> withScope(targetScope: Scope = Scope(parent = this.scope, errorHandler), block: () -> T) : T

    fun withScopeResolveBody(targetScope: Scope, body: BlockNode?)
}