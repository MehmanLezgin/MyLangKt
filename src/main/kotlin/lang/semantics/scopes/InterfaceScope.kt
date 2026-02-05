package lang.semantics.scopes

data class InterfaceScope(
    override val parent: Scope?,
    override val scopeName: String,
    override val superTypeScope: BaseTypeScope?
) : BaseTypeScope(
    parent = parent,
    scopeName = scopeName,
    superTypeScope = superTypeScope
)