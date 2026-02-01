package lang.semantics.scopes

import lang.messages.ErrorHandler

data class InterfaceScope(
    override val parent: Scope?,
    override val errorHandler: ErrorHandler,
    override val scopeName: String,
    override val superTypeScope: BaseTypeScope?
) : BaseTypeScope(
    parent = parent,
    errorHandler = errorHandler,
    scopeName = scopeName,
    superTypeScope = superTypeScope
)