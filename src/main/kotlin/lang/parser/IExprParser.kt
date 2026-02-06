package lang.parser

import lang.nodes.ExprNode
import lang.nodes.TypeNameListNode

interface IExprParser {
    fun parse(ctx: ParsingContext = ParsingContext.Default): ExprNode
    fun parseArgsList(ctx: ParsingContext): List<ExprNode>
    fun parseTypenameList(): TypeNameListNode?
    fun analiseAsDatatype(expr: ExprNode, allowAsExpression: Boolean = false): ExprNode?
    fun analiseTemplateList(exprList: List<ExprNode>?): TypeNameListNode?
}