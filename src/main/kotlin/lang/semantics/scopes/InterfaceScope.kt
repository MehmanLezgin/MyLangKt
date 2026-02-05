package lang.semantics.scopes

import lang.messages.ErrorHandler

data class InterfaceScope(
    override val parent: Scope?,
    override val scopeName: String,
    override val superTypeScope: BaseTypeScope?
) : BaseTypeScope(
    parent = parent,
    scopeName = scopeName,
    superTypeScope = superTypeScope
)