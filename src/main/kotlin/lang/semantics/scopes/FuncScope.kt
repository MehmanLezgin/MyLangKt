package lang.semantics.scopes

import lang.messages.ErrorHandler
import lang.semantics.symbols.FuncSymbol

class FuncScope(
    override val parent: Scope?,
    val funcSymbol: FuncSymbol,
    override val errorHandler: ErrorHandler
) : Scope(parent = parent, errorHandler = errorHandler) {


}