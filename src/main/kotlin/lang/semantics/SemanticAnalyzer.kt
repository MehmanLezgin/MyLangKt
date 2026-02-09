package lang.semantics

import lang.compiler.SourceUnit
import lang.compiler.SourceManager
import lang.core.SourceRange
import lang.mappers.ScopeErrorMapper
import lang.messages.CompileStage
import lang.messages.Msg
import lang.messages.MsgHandler
import lang.nodes.BlockNode
import lang.nodes.DeclStmtNode
import lang.nodes.ExprNode
import lang.nodes.ModuleStmtNode
import lang.semantics.resolvers.ConstResolver
import lang.semantics.resolvers.DeclarationResolver
import lang.semantics.resolvers.ModifierResolver
import lang.semantics.resolvers.TypeResolver
import lang.semantics.scopes.FileScope
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeError
import lang.semantics.scopes.ScopeResult
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.Symbol

class SemanticAnalyzer(
    override val msgHandler: MsgHandler,
    val moduleMgr: SourceManager
) : ISemanticAnalyzer {
    override var scope: Scope = Scope(parent = null)
    private var currentSourceUnit: SourceUnit? = null

    override val declResolver = DeclarationResolver(analyzer = this)
    override val constResolver = ConstResolver(analyzer = this)
    override val typeResolver = TypeResolver(analyzer = this)
    override val modResolver = ModifierResolver(analyzer = this)

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

        withModule(sourceUnit) {
            isAnalysing = true

            val fileScope = FileScope(parent = null, scopeName = sourceUnit.id)
            sourceUnit.scope = fileScope

            withScope(fileScope) {
                resolve(node = ast)
            }

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

    fun resolve(node: BlockNode) {
        node.nodes.forEach { childNode -> resolve(childNode) }
    }

    fun resolve(node: ExprNode) {
        when (node) {
            is DeclStmtNode<*> -> declResolver.resolve(node)
            is BlockNode -> resolve(node = node)
            else -> typeResolver.resolve(target = node)
        }
    }

    override fun registerModules(modules: List<ModuleStmtNode>) {
        modules.forEach(::registerModule)
    }

    fun registerModule(module: ModuleStmtNode) {
        val name = module.name
        val moduleSym = when (
            val result = scope.defineModuleIfNotExists(module)
        ) {
            is ScopeResult.Error -> {
                semanticError(Msg.CannotRegisterModule.format(name.value), module.name.range)
                return
            }

            is ScopeResult.Success<*> -> result.sym as ModuleSymbol
        }

        val nestedModules = module.nestedModules
        if (nestedModules.isEmpty()) return
        val moduleScope = moduleSym.scope

        withScope(moduleScope) {
            registerModules(nestedModules)
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

    fun withModule(
        targetSourceUnit: SourceUnit,
        block: SourceUnit.() -> Unit
    ) {
        val prev = currentSourceUnit
        currentSourceUnit = targetSourceUnit
        try {
            targetSourceUnit.apply(block)
        } finally {
            currentSourceUnit = prev
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