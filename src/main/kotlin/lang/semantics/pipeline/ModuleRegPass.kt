package lang.semantics.pipeline

import lang.nodes.BlockNode
import lang.nodes.IdentifierNode
import lang.nodes.ModuleStmtNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.resolvers.BaseResolver
import lang.semantics.scopes.FileScope
import lang.semantics.scopes.ModuleScope
import lang.semantics.scopes.Scope
import lang.semantics.scopes.SymbolMap
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.Symbol

class ModuleRegPass(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<BlockNode?, FileScope?>(analyzer = analyzer) {
    private val modules = mutableMapOf<String, ModuleSymbol>()

    var curScope: Scope? = null

    val allModules
        get() = modules.toMap()

    val allModulesAsSymbols: Map<String, Symbol>
        get() = modules.mapValues { it.value as Symbol }

    override fun resolve(target: BlockNode?) : FileScope? {
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

    fun getModule(nameNode: IdentifierNode): ModuleSymbol {
        val name = nameNode.value
        val existing = modules[name]

        fun newModule(sharedSymbols: SymbolMap = mutableMapOf()) =
            ModuleSymbol(
                name = name,
                scope = ModuleScope(
                    parent = curScope,
                    scopeName = name,
                    sharedSymbols = sharedSymbols
                )
            )

        if (existing == null)
            return newModule().also {
                modules[name] = it
            }

        if (curScope is FileScope)
            return newModule(existing.scope.symbols)


        return existing
    }

    private fun defineModuleIfNotExists(name: IdentifierNode): ModuleSymbol {
        val sym = getModule(name)
        curScope?.define(sym)?.handle(name.range) {}
        return sym
    }

    private fun registerModule(node: ModuleStmtNode) {
        val moduleSym = defineModuleIfNotExists(node.name)

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