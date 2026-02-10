package lang.semantics.scopes

open class ModuleScope(
    override val parent: Scope?,
    override val scopeName: String,
) : BaseTypeScope(
    parent = parent,
    scopeName = scopeName
)