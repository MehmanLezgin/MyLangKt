package lang.semantics

import lang.compiler.SourceUnit
import lang.core.SourceRange
import lang.messages.MsgHandler
import lang.nodes.BlockNode
import lang.semantics.pipeline.BindImportPass
import lang.semantics.pipeline.DeclarationHeaderPass
import lang.semantics.pipeline.ModuleRegPass
import lang.semantics.resolvers.ConstResolver
import lang.semantics.resolvers.DeclarationResolver
import lang.semantics.resolvers.ModifierResolver
import lang.semantics.pipeline.NameCollectionPass
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
    val nameCollectionPass: NameCollectionPass
    val declarationHeaderPass: DeclarationHeaderPass
    val bindImportPass: BindImportPass

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