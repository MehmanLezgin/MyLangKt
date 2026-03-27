package lang.semantics.scopes

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