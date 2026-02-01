package lang.semantics.scopes

import lang.messages.ErrorHandler

data class EnumScope(
    override val parent: Scope?,
    override val errorHandler: ErrorHandler,
    override val scopeName: String
) : BaseTypeScope(
    parent = parent,
    errorHandler = errorHandler,
    scopeName = scopeName,
    superTypeScope = null
)