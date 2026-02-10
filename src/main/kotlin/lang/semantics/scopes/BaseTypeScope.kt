package lang.semantics.scopes

open class BaseTypeScope(
    override val parent: Scope?,
    override val scopeName: String,
) : Scope(
    parent = parent,
    scopeName = scopeName
) {
    open val superTypeScope: BaseTypeScope? = null

    val instanceScope: Scope by lazy {
        Scope(
            parent = this,
            scopeName = null
        )
    }
}