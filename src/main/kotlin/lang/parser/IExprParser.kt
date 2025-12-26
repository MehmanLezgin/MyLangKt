package lang.parser

import lang.nodes.ExprNode

interface IExprParser {
    fun parse(ctx: ParsingContext = ParsingContext.Default): ExprNode
    fun parseArgsList(ctx: ParsingContext): List<ExprNode>
    fun parseTypenameList(): List<ExprNode>?
}