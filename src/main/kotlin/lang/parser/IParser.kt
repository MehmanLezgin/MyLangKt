package lang.parser

import lang.infrastructure.SourceRange
import lang.nodes.*
import lang.infrastructure.operators.OperatorType

interface IParser {
    fun parseSource(sourceId: String) : BlockNode
    fun parseExpr(ctx: ParsingContext = ParsingContext.Default) : ExprNode
    fun parseDatatype(startIdentifier: IdentifierNode? = null) : BaseDatatypeNode
    fun parseStmt(isSingleLine: Boolean = false): ExprNode
    fun parseArgsList(ctx: ParsingContext = ParsingContext.Header): List<ExprNode>
    fun syntaxError(msg: String, range: SourceRange)
    fun warning(msg: String, range: SourceRange)
    fun parseMultilineBlock(): BlockNode
    fun parseBlock(): BlockNode
    fun parseIdsWithSeparatorOper(separator: OperatorType): List<IdentifierNode>?
    fun parseNameClause(): NameClause
    fun parseNameSpecifier(): NameSpecifier?
    var moduleName: QualifiedName?
}