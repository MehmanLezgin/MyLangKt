package lang.semantics.scopes

import lang.semantics.builtin.PrimitivesScope
import lang.semantics.symbols.FuncSymbol
import lang.semantics.symbols.NamespaceSymbol
import lang.semantics.symbols.Symbol

class ModuleExportScope(
) : NamespaceScope(
    parent = PrimitivesScope,
    scopeName = "",
    isExport = true
) {
    val exportedSymbols: List<Symbol>
        get() = symbols.map { it.value }

    fun export(namePath: List<String>, sym: Symbol): ScopeResult {
        val targetScope = getNamespaceScope(namePath)
            ?: return ScopeError.CannotExport.err()

        return if (sym is FuncSymbol)
            targetScope.defineFunc(sym)
        else
            targetScope.define(sym)
    }

    private fun getNamespaceScope(namePath: List<String>): Scope? {
        if (namePath.isEmpty()) return this

        var currentScope: Scope = this

        namePath.forEach { name ->
            when (val result = getNamespace(name, currentScope)) {
                is ScopeResult.Error -> return null
                is ScopeResult.Success<*> -> {
                    if (result.sym !is NamespaceSymbol) return null
                    currentScope = result.sym.scope
                }
            }
        }

        return currentScope
    }

    private fun getNamespace(name: String, scope: Scope): ScopeResult {
        val result = scope.resolve(name)

        if (result is ScopeResult.Success<*> && result.sym is NamespaceSymbol)
            return result

        return createNamespaceSym(name, scope)
    }

    private fun createNamespaceSym(name: String, parent: Scope): ScopeResult {
        val sym = NamespaceSymbol(
            name = name,
            scope = NamespaceScope(
                parent = parent,
                scopeName = name,
                isExport = true
            )
        )

        return parent.define(sym)
    }
}