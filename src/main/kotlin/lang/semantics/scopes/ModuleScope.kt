package lang.semantics.scopes

import lang.semantics.symbols.Symbol

data class ModuleScope(
    override val parent: Scope?,
    override val scopeName: String,
    private val sharedSymbols: SymbolMap = mutableMapOf(),
) : BaseTypeScope(
    parent = parent,
    scopeName = scopeName
) {
    override val symbols = sharedSymbols
}