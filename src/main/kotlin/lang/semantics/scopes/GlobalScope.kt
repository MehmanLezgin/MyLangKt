package lang.semantics.scopes

import lang.messages.ErrorHandler
import lang.semantics.builtin.PrimitivesScope

class GlobalScope(
    override val errorHandler: ErrorHandler
) : Scope(
    parent = PrimitivesScope,
    errorHandler = errorHandler,
    scopeName = ""
)