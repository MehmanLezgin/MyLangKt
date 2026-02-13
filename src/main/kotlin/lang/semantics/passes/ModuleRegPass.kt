package lang.semantics.passes

import lang.compiler.SourceUnit
import lang.messages.Msg
import lang.nodes.ModuleStmtNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.resolvers.BaseResolver
import lang.semantics.scopes.FileScope
import lang.semantics.scopes.ModuleScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.ModuleSymbol

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

    private fun getModule(node: ModuleStmtNode): ModuleSymbol {
        val name = node.name.value

        val existingSym = modules[name]

        if (existingSym == null) {
            val sym = ModuleSymbol(
                name = name,
                scope = ModuleScope(
                    parent = curScope,
                    scopeName = name
                )
            )

            modules[name] = sym
            return sym
        }

        if (curScope is FileScope) {
            val sym = ModuleSymbol(
                name = name,
                scope = ModuleScope(
                    parent = curScope,
                    scopeName = name,
                    sharedSymbols = existingSym.scope.symbols
                )
            )
            return sym
        }

        return existingSym
    }

    private fun defineModuleIfNotExists(node: ModuleStmtNode): ModuleSymbol {
        val sym = getModule(node)
        curScope?.define(sym)?.handle(node.range) {}
        return sym
    }

    private fun registerModule(node: ModuleStmtNode) {
        val name = node.name

        val moduleSym = defineModuleIfNotExists(node)

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