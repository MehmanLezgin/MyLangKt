package lang.semantics

import lang.compiler.Module
import lang.compiler.ModuleManager
import lang.core.SourceRange
import lang.mappers.ScopeErrorMapper
import lang.messages.CompileStage
import lang.messages.Msg
import lang.messages.MsgHandler
import lang.nodes.BlockNode
import lang.nodes.DeclStmtNode
import lang.nodes.ExprNode
import lang.nodes.ImportModulesStmtNode
import lang.semantics.resolvers.ConstResolver
import lang.semantics.resolvers.DeclarationResolver
import lang.semantics.resolvers.ModifierResolver
import lang.semantics.resolvers.TypeResolver
import lang.semantics.scopes.ModuleScope
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeError
import lang.semantics.scopes.ScopeResult
import lang.semantics.symbols.Symbol

class SemanticAnalyzer(
    override val msgHandler: MsgHandler,
    val moduleMgr: ModuleManager
) : ISemanticAnalyzer {
    override var scope: Scope = Scope(parent = null)
    private var currentModule: Module? = null

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

    override fun resolve(module: Module) {
        if (module.isReady || module.isAnalysing) return

        withModule(module) {
            isAnalysing = true

            val moduleScope = ModuleScope(parent = null, scopeName = module.name)
            module.scope = moduleScope

            withScope(moduleScope) {
                resolve(node = ast.body)
            }

            isAnalysing = false
            isReady = true
        }
    }

    private fun checkModuleErrors(module: Module?): String? {
        return when {
            module == null -> Msg.MODULE_NOT_DEFINED
            module == currentModule -> Msg.MODULE_CANNOT_IMPORT_ITSELF
            module.isAnalysing -> Msg.IMPORT_CYCLE_NOT_ALLOWED
            else -> null
        }
    }

    fun getModule(name: String) = moduleMgr.modules[name]

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

    override fun exitScope() {
        val parent = scope.parent ?: return
        scope = parent
    }

    override fun enterScope(newScope: Scope) {
        scope = newScope
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
        targetModule: Module,
        block: Module.() -> Unit
    ) {
        val prev = currentModule
        currentModule = targetModule
        try {
            targetModule.apply(block)
        } finally {
            currentModule = prev
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