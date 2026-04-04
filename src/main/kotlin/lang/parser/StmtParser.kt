package lang.parser

import lang.infrastructure.KeywordType
import lang.infrastructure.LangSpec.moduleNameSeparator
import lang.infrastructure.SourceRange
import lang.infrastructure.operators.OperatorType
import lang.mappers.ModifierMapper
import lang.messages.Msg
import lang.nodes.*
import lang.parser.ParserUtils.isKeyword
import lang.parser.ParserUtils.isNotOperator
import lang.parser.ParserUtils.wrapToBlock
import lang.tokens.ITokenStream
import lang.tokens.Token

class StmtParser(
    private val ts: ITokenStream,
    private val parser: IParser,
) : IStmtParser {

    private val modifierMapper = ModifierMapper()

    private val declStmtParser: DeclarationStmtParser = DeclarationStmtParser(
        ts = ts,
        parser = parser
    )

    override fun parse(isSingleLine: Boolean): ExprNode {
        ts.skipTokens(Token.Semicolon::class)
        val t = ts.peek()

        if (t !is Token.Keyword) {

            if (ts.match(Token.EOF::class))
                return VoidNode

            if (ts.match(Token.RBrace::class)) {
                syntaxError(Msg.EXPECTED_TOP_LEVEL_DECL, t.range)
                ts.next()
                return VoidNode
            }

            if (ts.match(Token.RParen::class, Token.RBracket::class)) {
                syntaxError(Msg.UNEXPECTED_TOKEN, t.range)
                ts.next()
                return VoidNode
            }

            val expr = parser.parseExpr()

            if (!isSingleLine && !ts.match(Token.RBrace::class, Token.LBrace::class))
                ts.expectSemicolonOrLinebreak(Msg.EXPECTED_SEMICOLON)

            return expr
        }

        fun errorStmt(msg: String): VoidNode {
            syntaxError(msg, t.range)
            ts.next()
            return VoidNode
        }

        return when (t.type) {
            KeywordType.VAR,
            KeywordType.LET -> declStmtParser.parseVarDeclStmt()
            KeywordType.CLASS -> declStmtParser.parseClassStmt()
            KeywordType.INTERFACE -> declStmtParser.parseInterfaceStmt()
            KeywordType.ENUM -> declStmtParser.parseEnumStmt()
            KeywordType.FUNC -> declStmtParser.parseFuncDeclStmt()
            KeywordType.CONSTRUCTOR -> declStmtParser.parseConstructorStmt()
            KeywordType.DESTRUCTOR -> declStmtParser.parseDestructorStmt()

            KeywordType.DO -> parseDoWhileStmt()
            KeywordType.WHILE -> parseWhileStmt()
            KeywordType.MATCH -> parseMatchStmt()
            KeywordType.IF -> parseIfElseStmt()
            KeywordType.ELSE -> parseElseEntryStmt()
            KeywordType.ELIF -> errorStmt(Msg.EXPECTED_IF)

            KeywordType.PRIVATE,
            KeywordType.PUBLIC,
            KeywordType.INTERNAL,
            KeywordType.IMPLICIT,
            KeywordType.CONST, KeywordType.STATIC,
            KeywordType.OPEN, KeywordType.ABSTRACT,
            KeywordType.OVERRIDE, KeywordType.INFIX ->
                parseDeclarationWithModifiers()

            KeywordType.CONTINUE -> parseContinueStmt()
            KeywordType.BREAK -> parseBreakStmt()

            KeywordType.CATCH,
            KeywordType.FINALLY -> errorStmt(Msg.EXPECTED_TRY)

            KeywordType.RETURN -> parseReturnStmt()

            KeywordType.FOR -> parseForLoopStmt()
            KeywordType.TRY -> parseTryCatchStmt()
            KeywordType.MODULE -> parseModuleStmt()
            KeywordType.USING -> parseUsingStmt()
            KeywordType.OPERATOR -> errorStmt(Msg.EXPECTED_FUNC_DECL)
            KeywordType.IMPORT -> parseImportModuleStmt()
            KeywordType.FROM -> parseFromImportStmt()
        } ?: VoidNode
    }

    private fun parseUsingStmt(): StmtNode {
        return ts.captureRange {
            ts.next()

            if (ts.match(Token.LParen::class)) {
                val expr = parser.parseExpr(ctx = ParsingContext.Condition)
                val body = parseBlock()

                return@captureRange UsingStmtNode(
                    scopedExpr = expr,
                    body = body,
                    range = resultRange
                )
            }


            val clause = parser.parseNameClause()

            UsingDirectiveNode(
                clause = clause,
                range = resultRange
            )
        }
    }

    override fun parseMultilineBlock(): BlockNode {
        val list = mutableListOf<ExprNode>()

        return ts.captureRange {
            if (ts.match(Token.LBrace::class))
                ts.next()

            while (!ts.match(Token.RBrace::class, Token.EOF::class)) {
                val expr = parse()
                if (expr != VoidNode)
                    list.add(expr)
                ts.skipTokens(Token.Semicolon::class)
            }

            ts.expect(Token.RBrace::class, Msg.EXPECTED_RBRACE)

            BlockNode(nodes = list, range = resultRange)
        }
    }

    override fun parseBlock(): BlockNode {
        if (ts.matchOperator(OperatorType.COLON))
            ts.next()

        val isMultilineBody = ts.match(Token.LBrace::class)

        return if (isMultilineBody) {
            parseMultilineBlock()
        } else {
            val expr = parse(isSingleLine = true).wrapToBlock()
            ts.skipTokens(Token.Semicolon::class)
            expr
        }
    }

    private fun parseTryCatchStmt(): TryCatchStmtNode {
        return ts.captureRange {
            ts.next()
            val tryBody = parseBlock()

            var catchParam: ExprNode? = null
            var catchBody: BlockNode? = null
            var finallyBody: BlockNode? = null

            if (ts.peek() isKeyword KeywordType.CATCH) {
                ts.next()
                val pair = parseConditionAndBody()
                catchParam = pair.first
                catchBody = pair.second
            }

            if (ts.peek() isKeyword KeywordType.FINALLY) {
                ts.next()
                finallyBody = parseBlock()
            }

            if (catchBody == null && finallyBody == null) {
                syntaxError(Msg.EXPECTED_CATCH, ts.range)
            }

            TryCatchStmtNode(
                tryBody = tryBody,
                catchParam = catchParam,
                catchBody = catchBody,
                finallyBody = finallyBody,
                range = resultRange
            )
        }
    }

    private fun parseImportModuleStmt(): BaseImportStmtNode? {
        return ts.captureRange {
            ts.next()

            val clause = parser.parseNameClause()

            // example: import sym, ... from mod1::mod2
            if (ts.peek() isKeyword KeywordType.FROM) {
                ts.next()

                val sourceName = parser.parseNameSpecifier()
                    ?: return@captureRange null

                return@captureRange ImportFromStmtNode(
                    sourceName = sourceName,
                    items = clause,
                    range = resultRange
                )
            }

            // example: import mod1, ...
            ImportModulesStmtNode(
                items = clause,
                range = resultRange
            )
        }
    }


    private fun parseFromImportStmt(): ImportFromStmtNode? {
        return ts.captureRange {
            ts.next()

            val sourceName = parser.parseNameSpecifier()
                ?: return@captureRange null

            if (ts.expectKeyword(KeywordType.IMPORT, Msg.EXPECTED_IMPORT) == null)
                return@captureRange null

            ts.next()

            val clause = parser.parseNameClause()

            ImportFromStmtNode(
                sourceName = sourceName,
                items = clause,
                range = resultRange
            )
        }
    }

    private fun parseModuleStmt(): ModuleStmtNode? {
        return ts.captureRange {
            ts.next()

            val list = parser.parseIdsWithSeparatorOper(separator = moduleNameSeparator)
                ?.reversed()

            if (list.isNullOrEmpty()) return@captureRange null

            if (ts.match(Token.LBrace::class)) {
                val body = parseBlock()

                val module = buildModuleHierarchy(
                    list = list,
                    body = body,
                    range = resultRange
                )

                return@captureRange module
            } else {
                if (parser.moduleName != null) {
                    syntaxError(Msg.SRC_CAN_CONTAIN_ONE_FILE_MODULE_DECL, resultRange)
                } else
                    parser.moduleName = QualifiedName(list)

                null
            }
        }
    }

    override fun buildModuleHierarchy(
        list: List<IdentifierNode>,
        body: BlockNode,
        range: SourceRange
    ): ModuleStmtNode? {
        var module: ModuleStmtNode? = null

        list.forEachIndexed { i, id ->
            val moduleBody = if (i == 0) body else module!!.wrapToBlock()

            module = ModuleStmtNode(
                name = id,
                body = moduleBody,
                range = range
            )
        }

        return module
    }

    private fun parseContinueStmt() = ContinueStmtNode(range = ts.next().range)

    private fun parseBreakStmt() = BreakStmtNode(range = ts.next().range)

    private fun parseDeclarationWithModifiers(): StmtNode? {
        val modifiers = parseModifiers()
        val range = ts.range

        val stmtWithMod = when (val stmt = parse()) {
            is BaseDeclStmtNode -> {
                stmt.modifiers = modifiers
                stmt
            }

            else -> {
                syntaxError(Msg.EXPECTED_A_DECLARATION, range)
                null
            }
        }

        return stmtWithMod
    }

    private fun parseModifiers(): ModifierSetNode {
        return ts.captureRange {
            val modifiers = mutableSetOf<ModifierNode>()

            while (true) {
                val t = ts.peek()

                if (t !is Token.Keyword)
                    break

                val modifier = modifierMapper.toSecond(t) ?: break

                if (modifiers.any { it::class == modifier::class }) {
                    syntaxError(
                        Msg.RepeatedModifier.format(modifier.keyword.value),
                        t.range
                    )
                    ts.next()
                    continue
                }

                modifiers.add(modifier)
                ts.next()
            }

            ModifierSetNode(nodes = modifiers, range = resultRange)
        }
    }

    private fun parseConditionAndBody(): Pair<ExprNode, BlockNode> {
        val isConditionWithParens = ts.match(Token.LParen::class)

        val condition = parser.parseExpr(ctx = ParsingContext.Condition)

        if (!isConditionWithParens) {
            ts.expect(
                Token.LBrace::class,
                Token.Semicolon::class,
                msg = Msg.EXPECTED_LBRACE_AFTER_CONDITION
            )
        }

        val body = parseBlock()
        return condition to body
    }

    override fun parseIfElseStmt(): IfElseStmtNode {
        return ts.captureRange {
            ts.next()

            val (condition, body) = parseConditionAndBody()

            val t = ts.peek()

            val elseBody =
                when {
                    t isKeyword KeywordType.ELSE -> {
                        ts.next()
                        parseBlock()
                    }

                    t isKeyword KeywordType.ELIF -> {
                        parseIfElseStmt().wrapToBlock()
                    }

                    else -> null
                }

            IfElseStmtNode(
                condition = condition,
                body = body,
                elseBody = elseBody,
                range = resultRange
            )
        }
    }

    private fun parseWhileStmt(): WhileStmtNode {
        return ts.captureRange {
            ts.next()

            val (condition, body) = parseConditionAndBody()

            val elseBody = if (ts.peek() isKeyword KeywordType.ELSE) {
                ts.next()
                parseBlock()
            } else null

            WhileStmtNode(
                condition = condition,
                body = body,
                elseBody = elseBody,
                range = resultRange
            )
        }
    }

    private fun parseDoWhileStmt(): DoWhileStmtNode {
        return ts.captureRange {
            ts.next()

            val body = parseBlock()

            ts.expectKeyword(
                KeywordType.WHILE,
                Msg.EXPECTED_WHILE_AND_POST_CONDITION
            )

            val condition = parser.parseExpr(ctx = ParsingContext.Condition)

            DoWhileStmtNode(
                condition = condition,
                body = body,
                range = resultRange
            )
        }
    }

    private fun parseForLoopStmt(): ForLoopStmtNode {
        return ts.captureRange {
            ts.next()

            val (condition, body) = parseConditionAndBody()

            ForLoopStmtNode(
                condition = condition,
                body = body,
                range = resultRange
            )
        }
    }

    private fun parseElseEntryStmt(): ElseEntryNode? {
        return ts.captureRange {
            ts.next()
            val t = ts.peek()

            if (t isNotOperator OperatorType.ARROW) {
                syntaxError(Msg.EXPECTED_ARROW_OPERATOR, t.range)
                return@captureRange null
            }

            ts.next()

            ElseEntryNode(
                expr = parse(),
                range = resultRange
            )
        }
    }

    private fun parseMatchStmt(): MatchStmtNode {
        return ts.captureRange {
            ts.next()

            var target: ExprNode? = null

            val body = if (ts.match(Token.LBrace::class)) {
                parseBlock()
            } else {
                val (matchTargetNode, body) = parseConditionAndBody()
                target = matchTargetNode
                body
            }

            MatchStmtNode(
                target = target,
                body = body,
                range = resultRange
            )
        }
    }


    private fun parseReturnStmt(): ReturnStmtNode {
        return ts.captureRange {
            ts.next()

            val stmt = if (ts.matchSemicolonOrLinebreak())
                VoidNode
            else parse()

            ReturnStmtNode(
                expr = stmt,
                range = resultRange
            )
        }
    }

    private fun syntaxError(msg: String, range: SourceRange) =
        parser.syntaxError(msg = msg, range = range)
}