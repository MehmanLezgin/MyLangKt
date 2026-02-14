package lang.semantics.passes

import lang.compiler.SourceUnit
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
) : BaseResolver<List<ModuleStmtNode>, Unit>(analyzer = analyzer) {
    private val modules = mutableMapOf<String, ModuleSymbol>()

    var curScope: Scope? = null

    override fun resolve(target: List<ModuleStmtNode>) {
        target.forEach(::registerModule)
    }

    fun resolveForSource(sourceUnit: SourceUnit): FileScope {
        val modules = sourceUnit.ast.nodes.filterIsInstance<ModuleStmtNode>()
        val fileScope = FileScope(parent = PrimitivesScope, scopeName = null)

        withParent(fileScope) {
            resolve(modules)
        }

        return fileScope
    }

    val allModules
        get() = modules.toMap()

    val allModulesAsSymbols: Map<String, Symbol>
        get() = modules.mapValues { it.value as Symbol }

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
        val name = node.name

        val moduleSym = defineModuleIfNotExists(node.name)

//        if (moduleSym == null) {
//            semanticError(Msg.CannotRegisterModule.format(name.value), node.name.range)
//            return
//        }

        node bind moduleSym

        val nestedModules = node.nestedModules
        if (nestedModules.isEmpty()) return
        val moduleScope = moduleSym.scope

        withParent(moduleScope) {
            resolve(target = nestedModules)
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