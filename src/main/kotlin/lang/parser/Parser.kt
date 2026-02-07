package lang.parser

import lang.core.LangSpec.moduleNameSeparator
import lang.core.SourceRange
import lang.messages.CompileStage
import lang.messages.Msg
import lang.messages.MsgHandler
import lang.nodes.ExprNode
import lang.nodes.IdentifierNode
import lang.nodes.ModuleNode
import lang.parser.ParserUtils.isNotKeyword
import lang.parser.ParserUtils.isOperator
import lang.parser.ParserUtils.toIdentifierNode
import lang.tokens.ITokenStream
import lang.core.KeywordType
import lang.core.operators.OperatorType
import lang.tokens.Token

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

        return ts.captureRangeToCur {
            while (!ts.match(Token.EOF::class))
                list.add(stmtParser.parse())

            ModuleNode(
                name = name,
                nodes = list,
                range = resultRange
            )
        }
    }

    override fun parseExpr(ctx: ParsingContext) = exprParser.parse(ctx = ctx)

    override fun parseStmt(isSingleLine: Boolean) = stmtParser.parse(isSingleLine = isSingleLine)
    override fun parseTypenameList() = exprParser.parseTypenameList()
    override fun parseArgsList(ctx: ParsingContext) = exprParser.parseArgsList(ctx = ctx)
    override fun analiseParams(exprList: List<ExprNode>) = stmtParser.analiseParams(exprList)
    override fun analiseDatatypeList(exprList: List<ExprNode>?) = stmtParser.analiseDatatypeList(exprList)
    override fun parseBlock() = stmtParser.parseBlock()
    override fun analiseAsDatatype(expr: ExprNode, allowAsExpression: Boolean) =
        exprParser.analiseAsDatatype(expr, allowAsExpression)

    override fun analiseTemplateList(exprList: List<ExprNode>?) =
        exprParser.analiseTemplateList(exprList)


    override fun parseModuleName(withModuleKeyword: Boolean): IdentifierNode? {
        if (withModuleKeyword && ts.peek() isNotKeyword KeywordType.MODULE)
            return null

        if (withModuleKeyword)
            ts.next()

        return ts.captureRange {
            val list = parseIdsWithSeparatorOper(separator = moduleNameSeparator)

            if (withModuleKeyword)
                ts.expectSemicolonOrLinebreak(Msg.EXPECTED_SEMICOLON)

            val name = list?.joinToString(
                separator = moduleNameSeparator.raw
            ) { it.value } ?: return@captureRange null

            IdentifierNode(
                value = name,
                range = resultRange
            )
        }
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
                    syntaxError(Msg.EXPECTED_IDENTIFIER, identifier.range)
                    return null
                }

                add(identifier.toIdentifierNode())
                ts.next()
            } while (checkSeparator())
        }

        return list
    }

    override fun syntaxError(msg: String, range: SourceRange) =
        msgHandler.syntaxError(msg = msg, range = range)

    override fun warning(msg: String, range: SourceRange) =
        msgHandler.warn(msg = msg, range = range, stage = CompileStage.SYNTAX_ANALYSIS)
}