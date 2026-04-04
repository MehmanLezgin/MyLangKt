package lang.parser

import lang.core.KeywordType
import lang.core.SourceRange
import lang.core.operators.OperatorType
import lang.messages.Msg
import lang.nodes.*
import lang.parser.ParserUtils.isKeyword
import lang.parser.ParserUtils.isOperator
import lang.parser.ParserUtils.toIdentifierNode
import lang.tokens.ITokenStream
import lang.tokens.Token
import kotlin.reflect.KClass

class DeclarationStmtParser(
    private val ts: ITokenStream,
    private val parser: IParser
) {
    fun parseVarDeclStmt(): VarDeclStmtNode? {
        return ts.captureRange {
            val isMutable = ts.peek().let { t ->
                if (t isKeyword KeywordType.VAR) {
                    ts.next()
                    true
                } else if (t isKeyword KeywordType.LET) {
                    ts.next()
                    false
                } else false
            }

            val name = parseName() ?: return@captureRange null

            val datatype = if (ts.matchOperator(OperatorType.COLON)) {
                ts.next()
                parser.parseDatatype()
            } else
                AutoDatatypeNode(name.range)

            val initializer = if (ts.matchOperator(OperatorType.ASSIGN)) {
                ts.next()
                parser.parseExpr(ctx = ParsingContext.UntilComma)
            } else null

            VarDeclStmtNode(
                modifiers = null,
                name = name,
                datatype = datatype,
                initializer = initializer,
                isMutable = isMutable,
                range = resultRange
            )
        }
    }

    fun parseFuncDeclStmt(): FuncDeclStmtNode? {
        return ts.captureRange {
            ts.next()

            val typeNames = parseTemplateList()
//                if (ts.matchOperator(OperatorType.LESS))
//                parser.parseTypeArgsList() else null

//            val header = parser.parseExpr(ctx = ParsingContext.FuncHeader)
            val name = parseName() ?: return@captureRange null

            val params = parseParams()

            val returnType = if (ts.matchOperator(OperatorType.ARROW)) {
                ts.next()
                parser.parseDatatype()
            } else VoidDatatypeNode(range = name.range)

            val isExpressionBodied = ts.matchOperator(OperatorType.ASSIGN)
                .also { if (it) ts.next() }


            val body = if (isExpressionBodied)
                parser.parseBlock()
            else if (ts.match(Token.LBrace::class))
                parseBodyForDeclStmt()
            else
                null

            FuncDeclStmtNode(
                modifiers = null,
                name = name,
                params = params,
                templates = typeNames,
                returnType = returnType,
                isExpressionBodied = isExpressionBodied,
                body = body,
                range = resultRange
            )
        }
    }

    fun parseConstructorStmt(): ConstructorDeclStmtNode {
        return ts.captureRange {
            ts.next()

            var params: List<VarDeclStmtNode>? = null

            if (ts.match(Token.LParen::class))
                params = parseParams()

            val body = parseBodyForDeclStmt()

            ConstructorDeclStmtNode(
                modifiers = null,
                params = params ?: emptyList(),
                body = body,
                range = resultRange
            )
        }
    }

    fun parseDestructorStmt(): DestructorDeclStmtNode {
        return ts.captureRange {
            ts.next()

            if (ts.match(Token.LParen::class)) {
                val args = parser.parseArgsList() // skipping

                if (args.isNotEmpty())
                    syntaxError(Msg.CONSTRUCTORS_CANNOT_HAVE_PARAMS, startRange)
            }

            val body = parser.parseBlock()

            DestructorDeclStmtNode(
                modifiers = null,
                body = body,
                range = resultRange
            )
        }
    }

    fun parseClassStmt(): ClassDeclStmtNode? {
        return ts.captureRange {
            ts.next()

            val header = parser.parseExpr(ctx = ParsingContext.Header)

            val body = parseBodyForDeclStmt()

            /*buildClassStmt(
                header = header,
                body = body,
                range = resultRange
            )*/
            null
        }
    }

    fun parseInterfaceStmt(): InterfaceDeclStmtNode? {
        return ts.captureRange {
            ts.next()
            val header = parser.parseExpr(ctx = ParsingContext.Header)
            val body = parseBodyForDeclStmt()

            /*buildInterfaceStmt(
                header = header,
                body = body,
                range = resultRange
            )*/
            null
        }
    }

    fun parseEnumStmt(): EnumDeclStmtNode? {
        return ts.captureRange {
            ts.next()

            val nameToken = ts.peek()

            val enumName = if (nameToken is Token.Identifier) {
                ts.next()
                nameToken.toIdentifierNode()
            } else {
                syntaxError(Msg.NAME_EXPECTED, nameToken.range)
                return@captureRange null
            }

            if (!ts.match(Token.LBrace::class)) {
                syntaxError(Msg.UNEXPECTED_TOKEN, ts.range)
                ts.skipUntil(
                    Token.LBrace::class,
                    Token.RBrace::class,
                    Token.Keyword::class,
                    Token.Identifier::class
                )
                return@captureRange EnumDeclStmtNode(
                    modifiers = null,
                    name = enumName,
                    body = BlockNode.empty(resultRange),
                    range = resultRange
                )
            }

            val body = parseBodyForDeclStmt()

            EnumDeclStmtNode(
                modifiers = null,
                name = enumName,
                body = body,
                range = resultRange
            )
        }
    }

    private fun parseBodyForDeclStmt(): BlockNode? {
        return when {
            ts.match(Token.LBrace::class) -> parser.parseBlock()

            else -> {
                ts.expectSemicolonOrLinebreak(Msg.EXPECTED_SEMICOLON)
                null
            }
        }
    }

    private fun syntaxError(msg: String, range: SourceRange) =
        parser.syntaxError(msg = msg, range = range)

    private fun <T : ExprNode> parseExprList(
        isOpenToken: () -> Boolean,
        isCloseToken: () -> Boolean,
        parserFunction: () -> T?,
    ): List<T> {
        val list = mutableListOf<T>()

        if (!isOpenToken())
            return list

        ts.next()

        while (true) {
            if (isCloseToken()) {
                ts.next()
                break
            }

            val expr = parserFunction() ?: continue
            list.add(expr)

            if (ts.peek() isOperator OperatorType.COMMA)
                ts.next()
        }

        return list
    }

    private fun parseParams(): List<VarDeclStmtNode> {
        return parseExprList(
            isOpenToken = { ts.match(Token.LParen::class) },
            isCloseToken = { ts.match(Token.RParen::class) }
        ) {
            parseVarDeclStmt()
        }
    }

    private fun parseTemplateList(): TemplateParamsListNode {
        return ts.captureRange {
            val list = parseExprList(
                isOpenToken = { ts.matchOperator(OperatorType.LESS) },
                isCloseToken = { ts.matchOperator(OperatorType.GREATER) },
            ) {
                ts.captureRange {
                    val name = parseName() ?: run {
                        ts.next()
                        return@captureRange null
                    }

                    val bound = if (ts.matchOperator(OperatorType.COLON)) {
                        ts.next()
                        parser.parseDatatype()
                    } else null

                    TemplateParamNode(
                        name = name,
                        bound = bound,
                        range = resultRange
                    )
                }
            }

            TemplateParamsListNode(
                params = list,
                range = resultRange
            )
        }

    }

    private fun parseName(): IdentifierNode? {
        return ts.consume(Token.Identifier::class, Msg.EXPECTED_NAME)
            ?.toIdentifierNode()
    }
}
