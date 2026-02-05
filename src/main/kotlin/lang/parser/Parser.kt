package lang.parser

import lang.core.LangSpec.moduleNameSeparator
import lang.messages.CompileStage
import lang.messages.MsgHandler
import lang.messages.Msg
import lang.nodes.ExprNode
import lang.nodes.IdentifierNode
import lang.nodes.ModuleNode
import lang.parser.ParserUtils.isNotKeyword
import lang.parser.ParserUtils.isOperator
import lang.parser.ParserUtils.toIdentifierNode
import lang.tokens.*

class Parser(
    val ts: ITokenStream,
    val msgHandler: MsgHandler,
) : IParser {
    private val exprParser: IExprParser = ExprParser(
        ts = ts,
        parser = this
    )

    private val stmtParser: IStmtParser = StmtParser(
        ts = ts,
        parser = this
    )

    override fun parseModule(name: String): ModuleNode {
        val list = mutableListOf<ExprNode>()

        while (!ts.match(Token.EOF::class))
            list.add(stmtParser.parse())

        return ModuleNode(
            name = name,
            nodes = list,
            pos = ts.pos
        )
    }

    override fun parseExpr(ctx: ParsingContext) = exprParser.parse(ctx = ctx)

    override fun parseStmt(isSingleLine: Boolean) = stmtParser.parse(isSingleLine = isSingleLine)
    override fun parseTypenameList() = exprParser.parseTypenameList()
    override fun parseArgsList(ctx: ParsingContext) = exprParser.parseArgsList(ctx = ctx)
    override fun analiseParams(exprList: List<ExprNode>) = stmtParser.analiseParams(exprList)
    override fun analiseDatatypeList(exprList: List<ExprNode>?) = stmtParser.analiseDatatypeList(exprList)
    override fun parseBlock() = stmtParser.parseBlock()

    override fun parseModuleName(withModuleKeyword: Boolean): IdentifierNode? {
        if (withModuleKeyword && ts.peek() isNotKeyword KeywordType.MODULE)
            return null

        if (withModuleKeyword)
            ts.next()

        val pos = ts.pos
        val list = parseIdsWithSeparatorOper(separator = moduleNameSeparator)

        if (withModuleKeyword)
            ts.expectSemicolonOrLinebreak(Msg.EXPECTED_SEMICOLON)

        val name = list?.joinToString(
            separator = moduleNameSeparator.symbol
        ) { it.value } ?: return null

        return IdentifierNode(
            value = name,
            pos = pos
        )
    }

    override fun parseIdsWithSeparatorOper(separator: OperatorType): List<IdentifierNode>? {
        fun checkSeparator(): Boolean {
            val isSeparator = ts.peek() isOperator separator
            if (isSeparator) ts.next()
            return isSeparator
        }

        val list = mutableListOf<IdentifierNode>()
        list.apply {
            do {
                val identifier = ts.peek()

                if (identifier !is Token.Identifier) {
                    syntaxError(Msg.EXPECTED_IDENTIFIER, identifier.pos)
                    return null
                }

                add(identifier.toIdentifierNode())
                ts.next()
            } while (checkSeparator())
        }

        return list
    }

    override fun syntaxError(msg: String, pos: Pos) =
        msgHandler.syntaxError(msg = msg, pos = pos)

    override fun warning(msg: String, pos: Pos) =
        msgHandler.warn(msg = msg, pos = pos, stage = CompileStage.SYNTAX_ANALYSIS)
}