package lang.parser

import lang.core.LangSpec.moduleNameSeparator
import lang.core.SourceRange
import lang.core.operators.OperatorType
import lang.messages.CompileStage
import lang.messages.Msg
import lang.messages.MsgHandler
import lang.nodes.*
import lang.parser.ParserUtils.isOperator
import lang.parser.ParserUtils.toIdentifierNode
import lang.parser.ParserUtils.wrapToBlock
import lang.tokens.ITokenStream
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

    override var moduleName: QualifiedName? = null

    override fun parseSource(sourceId: String): BlockNode {
        val list = mutableListOf<ExprNode>()

        return ts.captureRangeToCur {
            while (!ts.match(Token.EOF::class)) {
                val expr = stmtParser.parse()
                if (expr != VoidNode)
                    list.add(expr)
            }

            val moduleName = this@Parser.moduleName
            val body = BlockNode(
                nodes = list,
                range = resultRange
            )

            if (moduleName != null) {
                val module = stmtParser.buildModuleHierarchy(
                    list = moduleName.parts,
                    body = body,
                    range = resultRange
                )!!

                return@captureRangeToCur module.wrapToBlock()
            }

            body
        }
    }

    /*override fun parseModuleName(withModuleKeyword: Boolean): NameSpecifier? {
        ts.save()
        var restored = false

        val result = run {
            if (withModuleKeyword && ts.peek() isNotKeyword KeywordType.MODULE)
                return@run null

            if (withModuleKeyword)
                ts.next()

            return@run ts.captureRange {
                val name = parseNameSpecifier()
                    ?: return@captureRange null

                if (ts.match(Token.LBrace::class)) {
                    ts.restore()
                    restored = true
                } else if (withModuleKeyword)
                    ts.expectSemicolonOrLinebreak(Msg.EXPECTED_SEMICOLON)

                return@captureRange name
            }
        }

        if (!restored)
            ts.clearLastSave()

        return result
    }*/

    override fun parseExpr(ctx: ParsingContext) = exprParser.parse(ctx = ctx)
    override fun parseDatatype() = exprParser.parse(ctx = ParsingContext.Datatype) as BaseDatatypeNode

    override fun parseStmt(isSingleLine: Boolean) = stmtParser.parse(isSingleLine = isSingleLine)
    override fun parseArgsList(ctx: ParsingContext) = exprParser.parseArgsList(ctx = ctx)
    override fun parseBlock() = stmtParser.parseBlock()
    override fun parseMultilineBlock() = stmtParser.parseMultilineBlock()

    override fun parseIdsWithSeparatorOper(separator: OperatorType): List<IdentifierNode>? {
        fun checkSeparator(): Boolean {
            val isSeparator = ts.peek() isOperator separator
            if (isSeparator) ts.next()
            val isMul = ts.peek() isOperator OperatorType.MUL
            return isSeparator && !isMul
        }

        val list = mutableListOf<IdentifierNode>()
        list.apply {
            do {
                val t = ts.peek()

                if (t !is Token.Identifier) {
                    syntaxError(Msg.EXPECTED_IDENTIFIER, t.range)
                    return null
                }

                add(t.toIdentifierNode())
                ts.next()
            } while (checkSeparator())
        }

        return list
    }

    override fun parseNameClause(): NameClause {
        val items = mutableListOf<NameSpecifier>()

        if (ts.matchOperator(OperatorType.MUL)) {
            ts.next()
            return NameClause.Wildcard
        }

        while (true) {
            val item = parseNameSpecifier() ?: break

            items.add(item)

            if (!ts.matchOperator(OperatorType.COMMA))
                break

            ts.next()
        }

        return NameClause.Items(items = items)
    }

    override fun parseNameSpecifier(): NameSpecifier? {
        val ids = parseIdsWithSeparatorOper(separator = moduleNameSeparator)
            ?: return null

        var alias: IdentifierNode? = null

        val isAllFrom = ts.peek() isOperator OperatorType.MUL
        if (isAllFrom) ts.next()


        if (!isAllFrom && ts.matchOperator(OperatorType.AS)) {
            ts.next()
            val id = ts.peek()
            if (id !is Token.Identifier) {
                syntaxError(Msg.EXPECTED_IDENTIFIER, id.range)
                return null
            }

            alias = id.toIdentifierNode()
            ts.next()
        }

        val target = QualifiedName(ids)

        if (isAllFrom) return NameSpecifier.AllFrom(
            target = target
        )

        if (alias != null) return NameSpecifier.Alias(
            target = target,
            alias = alias
        )

        return NameSpecifier.Direct(
            target = target
        )
    }

    override fun syntaxError(msg: String, range: SourceRange) =
        msgHandler.syntaxError(msg = msg, range = range)

    override fun warning(msg: String, range: SourceRange) =
        msgHandler.warn(msg = msg, range = range, stage = CompileStage.SYNTAX_ANALYSIS)
}