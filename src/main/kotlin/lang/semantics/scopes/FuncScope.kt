package lang.semantics.scopes

import lang.semantics.symbols.FuncSymbol

class FuncScope(
    override val parent: Scope?,
    val funcSymbol: FuncSymbol,
) : Scope(
    parent = parent,
    scopeName = ""
)