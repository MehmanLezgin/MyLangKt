package lang.semantics.scopes

import lang.messages.ErrorHandler

open class BaseTypeScope(
    override val parent: Scope?,
    override val errorHandler: ErrorHandler,
    override val scopeName: String,
    open val superTypeScope: BaseTypeScope?,
) : Scope(
    parent = parent,
    errorHandler = errorHandler,
    scopeName = scopeName
) {
    val instanceScope: Scope by lazy {
        Scope(
            parent = this,
            errorHandler = errorHandler,
            scopeName = null
        )
    }
}