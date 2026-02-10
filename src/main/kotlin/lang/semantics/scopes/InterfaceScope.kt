package lang.semantics.scopes

data class InterfaceScope(
    override val parent: Scope?,
    override val scopeName: String,
) : BaseTypeScope(
    parent = parent,
    scopeName = scopeName,
)