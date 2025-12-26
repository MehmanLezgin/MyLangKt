package lang.parser

import lang.tokens.ITokenStream
import lang.tokens.Pos
import lang.tokens.Token
import lang.messages.ErrorHandler
import lang.nodes.BlockNode
import lang.nodes.ExprNode

class Parser(
    val ts: ITokenStream,
    val errorHandler: ErrorHandler,
) : IParser {
    private val exprParser: IExprParser = ExprParser(
        ts = ts,
        parser = this
    )

    private val stmtParser: IStmtParser = StmtParser(
        ts = ts,
        parser = this
    )

    override fun parseFile(): BlockNode {
        val list = mutableListOf<ExprNode>()

        while (!ts.match(Token.EOF::class))
            list.add(stmtParser.parse())

        return BlockNode(list, ts.peek().pos)
    }

    override fun parseExpr(ctx: ParsingContext) = exprParser.parse(ctx = ctx)

    override fun parseStmt(isSingleLine: Boolean) = stmtParser.parse(isSingleLine = isSingleLine)
    override fun parseTypenameList() = exprParser.parseTypenameList()
    override fun parseArgsList(ctx: ParsingContext) = exprParser.parseArgsList(ctx = ctx)
    override fun analiseParams(exprList: List<ExprNode>) = stmtParser.analiseParams(exprList)
    override fun analiseDatatypeList(exprList: List<ExprNode>?) = stmtParser.analiseDatatypeList(exprList)
    override fun parseBlock() = stmtParser.parseBlock()

    override fun syntaxError(msg: String, pos: Pos) =
        errorHandler.syntaxError(msg = msg, pos = pos)

    override fun warning(msg: String, pos: Pos) =
        errorHandler.syntaxError(msg = msg, pos = pos)
}