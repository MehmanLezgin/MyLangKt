package lang.semantics.scopes

data class ClassScope(
    override val parent: Scope?,
    override val scopeName: String
) : BaseTypeScope(
    parent = parent,
    scopeName = scopeName,
) {
    override fun toString(): String {
        return ""
    }
}