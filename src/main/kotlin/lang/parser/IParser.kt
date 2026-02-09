package lang.parser

import lang.core.SourceRange
import lang.nodes.*
import lang.core.operators.OperatorType

interface IParser {
    fun parseModule(name: IdentifierNode) : ModuleStmtNode
    fun parseExpr(ctx: ParsingContext = ParsingContext.Default) : ExprNode
    fun parseStmt(isSingleLine: Boolean = false): ExprNode
    fun parseTypenameList(): TypeNameListNode?
    fun parseArgsList(ctx: ParsingContext = ParsingContext.Header): List<ExprNode>
    fun analiseParams(exprList: List<ExprNode>): List<VarDeclStmtNode>?
    fun syntaxError(msg: String, range: SourceRange)
    fun warning(msg: String, range: SourceRange)
    fun analiseDatatypeList(exprList: List<ExprNode>?): List<BaseDatatypeNode>?
    fun parseBlock(): BlockNode
    fun parseModuleName(withModuleKeyword: Boolean = true): NameSpecifier?
    fun parseIdsWithSeparatorOper(separator: OperatorType): List<IdentifierNode>?
    fun analiseAsDatatype(expr: ExprNode, allowAsExpression: Boolean = false): ExprNode?
    fun analiseTemplateList(exprList: List<ExprNode>?): TypeNameListNode?
    fun parseNameClause(): NameClause
    fun parseNameSpecifier(): NameSpecifier?
}