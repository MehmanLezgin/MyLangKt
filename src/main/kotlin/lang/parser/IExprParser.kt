package lang.parser

import lang.nodes.BaseDatatypeNode
import lang.nodes.ExprNode
import lang.nodes.IdentifierNode
import lang.nodes.TemplateParamsListNode
import lang.nodes.TypeArgsListNode

interface IExprParser {
    fun parse(ctx: ParsingContext = ParsingContext.Default): ExprNode
    fun parseDatatype(
        startIdentifier: IdentifierNode? = null,
        ctx: ParsingContext = ParsingContext.Datatype
    ): BaseDatatypeNode

    fun parseArgsList(ctx: ParsingContext): List<ExprNode>
    fun parseTypeArgsList(): TypeArgsListNode?
    fun analiseAsDatatype(expr: ExprNode, allowAsExpression: Boolean = false): ExprNode?
}