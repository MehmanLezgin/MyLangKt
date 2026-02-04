package lang.semantics.scopes

import lang.messages.ErrorHandler
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.symbols.FuncSymbol
import lang.semantics.symbols.NamespaceSymbol
import lang.semantics.symbols.Symbol
import lang.tokens.Pos

class ModuleExportScope(
    errorHandler: ErrorHandler = ErrorHandler()
) : NamespaceScope(
    parent = PrimitivesScope,
    errorHandler = errorHandler,
    scopeName = "",
    isExport = true
) {
    val exportedSymbols: List<Symbol>
        get() = symbols.map { it.value }

    fun export(namePath: List<String>, sym: Symbol, pos: Pos?): Symbol {
        val targetScope = getNamespaceScope(namePath)

        return if (sym is FuncSymbol)
            targetScope.defineFunc(sym, pos)
        else
            targetScope.define(sym, pos)
    }

    private fun getNamespaceScope(namePath: List<String>): Scope {
        if (namePath.isEmpty()) return this

        var currentScope: Scope = this

        namePath.forEach { name ->
            val sym = getNamespace(name, currentScope)
            currentScope = sym.scope
        }

        return currentScope
    }

    private fun getNamespace(name: String, scope: Scope): NamespaceSymbol {
        val sym = scope.resolve(name)
//            ?: createNamespaceSym(name, scope)

        if (sym is NamespaceSymbol) return sym

        return createNamespaceSym(name, scope)
    }

    private fun createNamespaceSym(name: String, parent: Scope): NamespaceSymbol {
        val sym = NamespaceSymbol(
            name = name,
            scope = NamespaceScope(
                parent = parent,
                errorHandler = errorHandler,
                scopeName = name,
                isExport = true
            )
        ).also { parent.define(it, null) }

        return sym
    }
}