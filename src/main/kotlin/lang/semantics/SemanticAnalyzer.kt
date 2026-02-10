package lang.semantics

import lang.compiler.SourceManager
import lang.compiler.SourceUnit
import lang.core.SourceRange
import lang.mappers.ScopeErrorMapper
import lang.messages.CompileStage
import lang.messages.Msg
import lang.messages.MsgHandler
import lang.nodes.*
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.resolvers.ConstResolver
import lang.semantics.resolvers.DeclarationResolver
import lang.semantics.resolvers.ModifierResolver
import lang.semantics.resolvers.TypeResolver
import lang.semantics.scopes.ModuleScope
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeError
import lang.semantics.scopes.ScopeResult
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.Symbol
import lang.semantics.types.Type

class SemanticAnalyzer(
    override val msgHandler: MsgHandler,
    val moduleMgr: SourceManager
) : ISemanticAnalyzer {
    override var scope: Scope = Scope(parent = PrimitivesScope)
    private val rootModuleScope = ModuleScope(parent = scope, scopeName = "global")
    private val rootModule = ModuleSymbol(name = rootModuleScope.scopeName, scope = rootModuleScope)

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

    override fun resolveModule(name: String): ModuleSymbol? {
        val result = rootModuleScope.resolveModule(name)
        if (result is ScopeResult.Success<*>)
            return result.sym as ModuleSymbol

        return null
    }

    private fun registerModules(modules: List<ModuleStmtNode>) {
        modules.forEach(::registerModule)
    }

    private fun registerModule(module: ModuleStmtNode) {
        val name = module.name

        val moduleSym = when (
            val result = rootModuleScope.defineModuleIfNotExists(module)
        ) {
            is ScopeResult.Error -> {
                semanticError(Msg.CannotRegisterModule.format(name.value), module.name.range)
                return
            }

            is ScopeResult.Success<*> -> result.sym as ModuleSymbol
        }

        module bind moduleSym

        val nestedModules = module.nestedModules
        if (nestedModules.isEmpty()) return
        val moduleScope = moduleSym.scope

        withScope(moduleScope) {
            registerModules(nestedModules)
        }
    }

    override fun registerSources(sources: List<SourceUnit>) {
        val allModules = sources.flatMap {
            it.ast.nodes.filterIsInstance<ModuleStmtNode>()
        }

        registerModules(modules = allModules)

        /*val imports = sourceUnit.imports
        if (imports.isEmpty()) return

        imports.forEach { importNode ->
            when (importNode) {
                is ImportModulesStmtNode -> {}
                is ImportFromStmtNode -> {}
            }
        }*/
    }

    private fun registerModulesInSourceUnit(
        clause: NameClause,
        sourceUnit: SourceUnit
    ) {

    }

    private fun registerModuleInSourceUnit(
        name: QualifiedName,
        alias: IdentifierNode,
        sourceUnit: SourceUnit
    ) {

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

    infix fun ExprNode.attach(type: Type?) {
        if (type == null) return
        semanticContext.types[this] = type
    }

    infix fun ExprNode.bind(symbol: Symbol?) {
        if (symbol == null) return
        semanticContext.symbols[this] = symbol
    }
}