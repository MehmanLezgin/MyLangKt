package lang.parser

import lang.nodes.ExprNode
import lang.nodes.TemplateParamsListNode
import lang.nodes.TypeArgsListNode

interface IExprParser {
    fun parse(ctx: ParsingContext = ParsingContext.Default): ExprNode
    fun parseArgsList(ctx: ParsingContext): List<ExprNode>
    fun parseTypeArgsList(): TypeArgsListNode?
    fun analiseAsDatatype(expr: ExprNode, allowAsExpression: Boolean = false): ExprNode?
}