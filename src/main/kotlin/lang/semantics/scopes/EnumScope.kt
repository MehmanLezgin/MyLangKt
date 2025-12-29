package lang.semantics.scopes

import lang.messages.ErrorHandler

data class EnumScope(
    override val parent: Scope?,
    override val errorHandler: ErrorHandler
) : Scope(
    parent = parent,
    errorHandler = errorHandler
)