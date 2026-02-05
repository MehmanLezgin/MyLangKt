package lang.semantics.scopes

open class BaseTypeScope(
    override val parent: Scope?,
    override val scopeName: String,
    open val superTypeScope: BaseTypeScope?,
) : Scope(
    parent = parent,
    scopeName = scopeName
) {
    val instanceScope: Scope by lazy {
        Scope(
            parent = this,
            scopeName = null
        )
    }
}