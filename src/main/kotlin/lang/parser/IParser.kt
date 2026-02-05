package lang.parser

import lang.core.SourceRange
import lang.nodes.*
import lang.tokens.OperatorType

interface IParser {
    fun parseModule(name: String) : ModuleNode
    fun parseExpr(ctx: ParsingContext = ParsingContext.Default) : ExprNode
    fun parseStmt(isSingleLine: Boolean = false): ExprNode
    fun parseTypenameList(): List<ExprNode>?
    fun parseArgsList(ctx: ParsingContext = ParsingContext.Header): List<ExprNode>
    fun analiseParams(exprList: List<ExprNode>): List<VarDeclStmtNode>?
    fun syntaxError(msg: String, range: SourceRange)
    fun warning(msg: String, range: SourceRange)
    fun analiseDatatypeList(exprList: List<ExprNode>?): List<BaseDatatypeNode>?
    fun parseBlock(): BlockNode
    fun parseModuleName(withModuleKeyword: Boolean = true): IdentifierNode?
    fun parseIdsWithSeparatorOper(separator: OperatorType): List<IdentifierNode>?
}