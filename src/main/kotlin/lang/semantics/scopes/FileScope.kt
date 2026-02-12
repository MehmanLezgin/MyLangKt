package lang.semantics.scopes

open class FileScope(
    override val parent: Scope?,
    override val scopeName: String?,
) : Scope(
    parent = parent,
    scopeName = scopeName
)