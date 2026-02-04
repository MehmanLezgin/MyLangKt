package lang.semantics.scopes

import lang.messages.ErrorHandler
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.symbols.Symbol

class ModuleScope(
    override val errorHandler: ErrorHandler
) : NamespaceScope(
    parent = PrimitivesScope,
    errorHandler = errorHandler,
    scopeName = "",
    isExport = true
)