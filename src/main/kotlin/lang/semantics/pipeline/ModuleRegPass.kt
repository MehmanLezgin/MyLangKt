package lang.semantics.pipeline

import lang.nodes.BlockNode
import lang.nodes.ModuleStmtNode
import lang.semantics.ISemanticAnalyzer
import lang.core.PrimitivesScope
import lang.semantics.resolvers.BaseResolver
import lang.semantics.scopes.FileScope
import lang.semantics.scopes.ModuleScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.Modifiers
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.Symbol

class ModuleRegPass(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<BlockNode?, FileScope?>(analyzer = analyzer) {
    val modulesContainer = ModuleScope(parent = null, scopeName = "")

    init {
        modulesContainer.defineModules(
            modules = PrimitivesScope.builtInModules
        )
    }

    var curScope: Scope? = null

    override fun resolve(target: BlockNode?): FileScope? {
        if (target == null) return null
        val fileScope = FileScope(parent = PrimitivesScope, scopeName = null)

        withParent(fileScope) {
            target.nodes.forEach { node ->
                when (node) {
                    is ModuleStmtNode -> registerModule(node = node)
                    else -> Unit
                }
            }
        }

        return fileScope
    }

    private fun createModule(
        name: String,
        sharedSymbols: MutableMap<String, Symbol> = mutableMapOf(),
        modifiers: Modifiers,
    ): ModuleSymbol {
        val moduleScope = ModuleScope(
            parent = curScope,
            scopeName = name,
            sharedSymbols = sharedSymbols
        )

        val moduleSym = ModuleSymbol(
            name = name,
            scope = moduleScope,
            modifiers = modifiers
        )

        moduleScope.ownerSymbol = moduleSym

        return moduleSym
    }


    fun getOrCreateModule(name: String, modifiers: Modifiers): ModuleSymbol {
        val existing = modulesContainer.resolveModule(name = name, fromScope = scope)
            .handle(null) {
                sym as ModuleSymbol
            }

        if (existing == null)
            return createModule(name = name, modifiers = modifiers).also { module ->
                modulesContainer.define(sym = module)
            }

        if (curScope is FileScope)
            return createModule(
                name = name,
                sharedSymbols = existing.scope.symbols,
                modifiers = modifiers
            )

        return existing
    }

    private fun registerModule(node: ModuleStmtNode) {
        val moduleSym = getOrCreateModule(
            name = node.name.value,
            modifiers = analyzer.modResolver.resolveModuleModifiers(node.modifiers)
        )

        curScope?.define(moduleSym)

        node bind moduleSym

        val nestedModules = node.nestedModules
        if (nestedModules.isEmpty()) return
        val moduleScope = moduleSym.scope

        withParent(moduleScope) {
            nestedModules.forEach(::registerModule)
        }
    }

    private fun <T> withParent(
        targetScope: Scope,
        block: () -> T
    ): T {
        val prev = curScope
        curScope = targetScope
        try {
            return block()
        } finally {
            curScope = prev
        }
    }
}