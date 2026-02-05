package lang.semantics.scopes

open class NamespaceScope(
    override val parent: Scope?,
    override val scopeName: String,
    val isExport: Boolean
) : BaseTypeScope(
    parent = parent,
    scopeName = scopeName,
    superTypeScope = null
)