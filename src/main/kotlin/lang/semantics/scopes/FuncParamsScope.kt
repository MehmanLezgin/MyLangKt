package lang.semantics.scopes

data class FuncParamsScope(
    override val parent: Scope?,
) : Scope(
    parent = parent,
    scopeName = ""
)