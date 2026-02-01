package lang.semantics.scopes

import lang.messages.ErrorHandler
import lang.nodes.ClassDeclStmtNode
import lang.nodes.EnumDeclStmtNode
import lang.nodes.InterfaceDeclStmtNode
import lang.semantics.symbols.ClassSymbol
import lang.semantics.symbols.EnumSymbol
import lang.semantics.symbols.InterfaceSymbol
import lang.semantics.symbols.Symbol

class GlobalScope(
    override val errorHandler: ErrorHandler
) : Scope(
    parent = null,
    errorHandler = errorHandler,
    scopeName = ""
)