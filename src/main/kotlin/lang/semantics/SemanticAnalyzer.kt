package lang.semantics

import lang.compiler.Module
import lang.compiler.ModuleManager
import lang.messages.ErrorHandler
import lang.messages.Messages
import lang.nodes.*
import lang.semantics.resolvers.ConstResolver
import lang.semantics.resolvers.DeclarationResolver
import lang.semantics.resolvers.TypeResolver
import lang.semantics.scopes.ModuleScope
import lang.semantics.scopes.NamespaceScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.NamespaceSymbol
import lang.semantics.symbols.Symbol
import lang.tokens.Pos

class SemanticAnalyzer(
    override val errorHandler: ErrorHandler,
    val moduleMgr: ModuleManager
) : ISemanticAnalyzer {
    override var scope: Scope = Scope(errorHandler = errorHandler, parent = null)
    private var currentModule: Module? = null

    override val declResolver: DeclarationResolver = DeclarationResolver(analyzer = this)
    override val constResolver: ConstResolver = ConstResolver(analyzer = this)
    override val typeResolver: TypeResolver = TypeResolver(analyzer = this)

    override val semanticContext = SemanticContext()


    fun importSymbol(sym: Symbol, pos: Pos) {
        scope.define(sym, pos)
    }

    override fun exportSymbol(sym: Symbol) {
        if (sym is NamespaceSymbol) return
        val module = currentModule ?: return

        val namePath = ArrayDeque<String>()
        var cur: Scope? = scope

        while (cur !is ModuleScope) {
            if (cur !is NamespaceScope) {
                semanticError("cannot export ${sym.name}", Pos())
                return
            }
            namePath.addFirst(cur.scopeName) // сразу в правильном порядке
            cur = cur.parent
        }

        module.export(sym, namePath)
    }

    override fun resolve(module: Module) {
        if (module.isReady || module.isAnalysing) return

        withModule(module) {
            isAnalysing = true

            val moduleScope = ModuleScope(errorHandler = errorHandler)
            module.scope = moduleScope

            withScope(moduleScope) {
                resolve(node = ast as BlockNode)
            }

            isAnalysing = false
            isReady = true
        }
    }

    fun resolve(node: ImportStmtNode) {
        val name = node.moduleName
        val module = getModule(name.value)

        when {
            module == null -> Messages.MODULE_NOT_DEFINED
            module == currentModule -> Messages.MODULE_CANNOT_IMPORT_ITSELF
            module.isAnalysing -> Messages.IMPORT_CYCLE_NOT_ALLOWED
            else -> null
        }?.let {
            semanticError(it, name.pos)
            return
        }
        module!!

        resolve(module = module)

        val allExports = module.exportsScope.symbols
        val moduleName = node.moduleName.value

        if (node.kind == ImportKind.Module) {
            val sameNameSym = allExports[moduleName]

            val sym = when {
                sameNameSym != null -> sameNameSym

                else -> ModuleSymbol(
                    name = moduleName,
                    scope = module.exportsScope
                )
            }

            importSymbol(sym, node.moduleName.pos)
            return
        }

        when (node.kind) {
            is ImportKind.Named -> node.kind.symbols
            ImportKind.Wildcard ->
                allExports.map {
                    IdentifierNode(it.key, node.pos)
                }
            else -> emptyList()
        }.forEach { id ->
            val sym = allExports[id.value]

            if (sym == null) {
                semanticError(Messages.F_MODULE_DOES_NOT_EXPORT_SYM.format(moduleName, id.value), id.pos)
                return@forEach
            }

            importSymbol(sym, id.pos)
        }
    }

    fun getModule(name: String) = moduleMgr.modules[name]

    fun resolve(node: BlockNode) {
        node.nodes.forEach { childNode -> resolve(childNode) }
    }


    fun resolve(node: ExprNode) {
        when (node) {
            is DeclStmtNode -> declResolver.resolve(node)
            is BlockNode -> resolve(node = node)
            is ImportStmtNode -> resolve(node = node)
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

    private fun semanticError(msg: String, pos: Pos?) {
        errorHandler.semanticError(msg, pos)
    }
}