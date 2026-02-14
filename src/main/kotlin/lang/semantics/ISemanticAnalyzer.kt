package lang.semantics

import lang.compiler.SourceUnit
import lang.core.SourceRange
import lang.messages.MsgHandler
import lang.nodes.BlockNode
import lang.semantics.passes.BindImportPass
import lang.semantics.passes.ModuleRegPass
import lang.semantics.resolvers.ConstResolver
import lang.semantics.resolvers.DeclarationResolver
import lang.semantics.resolvers.ModifierResolver
import lang.semantics.passes.TypeCollectionPass
import lang.semantics.passes.TypeHierarchyPass
import lang.semantics.resolvers.TypeResolver
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeError

interface ISemanticAnalyzer {
    val scope: Scope
    val msgHandler: MsgHandler

    val declResolver: DeclarationResolver
    val constResolver: ConstResolver
    val typeResolver: TypeResolver
    val modResolver: ModifierResolver

    val moduleRegPass: ModuleRegPass
    val typeCollectionPass: TypeCollectionPass
    val bindImportPass: BindImportPass
    val typeHierarchyPass: TypeHierarchyPass

    val semanticContext: SemanticContext

    fun resolve(sourceUnit: SourceUnit)
    fun <T> withScope(targetScope: Scope = Scope(parent = this.scope), block: () -> T): T

    fun withScopeResolveBody(targetScope: Scope, body: BlockNode?)

    fun scopeError(error: ScopeError, range: SourceRange?)
    fun warning(msg: String, range: SourceRange)
//    fun getModule(name: String): ModuleSymbol?
    fun registerSources(sources: List<SourceUnit>)
    fun resolve(node: BlockNode)
}