package lang.semantics

import lang.compiler.SourceUnit
import lang.core.SourceRange
import lang.mappers.ScopeErrorMapper
import lang.messages.CompileStage
import lang.messages.Msg
import lang.messages.MsgHandler
import lang.nodes.BlockNode
import lang.nodes.DeclStmtNode
import lang.nodes.ExprNode
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.pipeline.BindImportPass
import lang.semantics.pipeline.ModuleRegPass
import lang.semantics.pipeline.TypeCollectionPass
import lang.semantics.pipeline.TypeHierarchyPass
import lang.semantics.resolvers.ConstResolver
import lang.semantics.resolvers.DeclarationResolver
import lang.semantics.resolvers.ModifierResolver
import lang.semantics.resolvers.TypeResolver
import lang.semantics.scopes.FileScope
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeError
import lang.semantics.scopes.ScopeResult
import lang.semantics.symbols.Symbol

class SemanticAnalyzer(
    override val msgHandler: MsgHandler,
) : ISemanticAnalyzer {
    override var scope: Scope = Scope(parent = PrimitivesScope)

    private var currentSourceUnit: SourceUnit? = null

    override val declResolver = DeclarationResolver(analyzer = this)
    override val constResolver = ConstResolver(analyzer = this)
    override val typeResolver = TypeResolver(analyzer = this)
    override val modResolver = ModifierResolver(analyzer = this)

    override val typeCollectionPass = TypeCollectionPass(analyzer = this)
    override val moduleRegPass = ModuleRegPass(analyzer = this)
    override val bindImportPass = BindImportPass(analyzer = this, moduleRegPass = moduleRegPass)
    override val typeHierarchyPass = TypeHierarchyPass(analyzer = this)

    override val semanticContext = SemanticContext()

    override fun scopeError(error: ScopeError, range: SourceRange?) {
        semanticError(msg = ScopeErrorMapper.toSecond(error), range = range)
    }

    fun importSymbol(sym: Symbol, range: SourceRange) {
        val result = scope.define(sym)
        if (result !is ScopeResult.Error) return
        scopeError(error = result.error, range = range)
    }

    override fun resolve(sourceUnit: SourceUnit) {
        if (sourceUnit.isReady || sourceUnit.isAnalysing) return

        withSourceUnit(targetSourceUnit = sourceUnit) {
            isAnalysing = true

            resolve(node = ast)

            isAnalysing = false
            isReady = true
        }
    }

    private fun checkModuleErrors(sourceUnit: SourceUnit?): String? {
        return when {
            sourceUnit == null -> Msg.MODULE_NOT_DEFINED
            sourceUnit == currentSourceUnit -> Msg.MODULE_CANNOT_IMPORT_ITSELF
            sourceUnit.isAnalysing -> Msg.IMPORT_CYCLE_NOT_ALLOWED
            else -> null
        }
    }

    override fun resolve(node: BlockNode) {
        node.nodes.forEach { childNode -> resolve(childNode) }
    }

    fun resolve(node: ExprNode) {
        when (node) {
            is DeclStmtNode -> declResolver.resolve(node)
            is BlockNode -> resolve(node = node)
            else -> typeResolver.resolve(target = node)
        }
    }

//    override fun getModule(name: String): ModuleSymbol? {
//        return moduleRegPass.getModule(name)
//    }

    override fun registerSources(sources: List<SourceUnit>) {
        sources.forEach {
            it.scope = moduleRegPass.resolveForSource(sourceUnit = it)
        }

        sources.forEach {
            withScope(it.scope!!) {
                typeCollectionPass.resolve(target = it.ast)
            }
        }

        sources.forEach {
            withScope(it.scope!!) {
                bindImportPass.resolve(target = it.ast)
            }
        }

        sources.forEach {
            withScope(it.scope!!) {
                typeHierarchyPass.resolve(target = it.ast)
            }
        }
    }

    override fun <T> withScope(
        targetScope: Scope,
        block: () -> T
    ): T {
        val prev = scope
        scope = targetScope
        try {
            return block()
        } finally {
            scope = prev
        }
    }

    fun withSourceUnit(
        targetSourceUnit: SourceUnit,
        block: SourceUnit.() -> Unit
    ) {
        val targetScope = targetSourceUnit.scope
            ?: FileScope(parent = scope, scopeName = "")

        withScope(targetScope) {
            val prev = currentSourceUnit
            currentSourceUnit = targetSourceUnit
            try {
                targetSourceUnit.apply(block)
            } finally {
                currentSourceUnit = prev
            }
        }
    }


    override fun withScopeResolveBody(targetScope: Scope, body: BlockNode?) {
        if (body == null) return
        withScope(targetScope = targetScope) {
            resolve(node = body)
        }
    }

    private fun semanticError(msg: String, range: SourceRange?) {
        msgHandler.semanticError(msg, range)
    }

    override fun warning(msg: String, range: SourceRange) =
        msgHandler.warn(msg = msg, range = range, stage = CompileStage.SEMANTIC_ANALYSIS)
}