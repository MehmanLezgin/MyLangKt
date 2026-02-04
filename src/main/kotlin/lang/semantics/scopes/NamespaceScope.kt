package lang.semantics.scopes

import lang.messages.ErrorHandler
import lang.nodes.ConstructorDeclStmtNode
import lang.nodes.DestructorDeclStmtNode
import lang.semantics.symbols.ClassSymbol
import lang.semantics.symbols.Symbol

open class NamespaceScope(
    override val parent: Scope?,
    override val errorHandler: ErrorHandler,
    override val scopeName: String,
    val isExport: Boolean
) : BaseTypeScope(
    parent = parent,
    errorHandler = errorHandler,
    scopeName = scopeName,
    superTypeScope = null
)