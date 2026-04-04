package lang.parser

import lang.infrastructure.KeywordType
import lang.infrastructure.SourceRange
import lang.infrastructure.operators.OperatorType
import lang.messages.Msg
import lang.messages.Terms
import lang.nodes.*
import lang.parser.ParserUtils.isKeyword
import lang.parser.ParserUtils.toDatatype
import lang.parser.ParserUtils.toIdentifierNode
import lang.tokens.ITokenStream
import lang.tokens.Token

class DeclarationStmtParser(
    private val ts: ITokenStream,
    private val parser: IParser
) {
    private fun handleVarMutability(asParam: Boolean): Boolean {
        val t = ts.peek()

        val mutType = when {
            t isKeyword KeywordType.VAR -> KeywordType.VAR
            t isKeyword KeywordType.LET -> KeywordType.LET
            else -> null
        }

        return when {
            asParam && mutType != null -> {
                syntaxError(Msg.MutModNotAllowedInParam.format(name = t.raw), t.range)
                ts.next(); false
            }

            mutType == KeywordType.VAR -> {
                ts.next(); true
            }

            mutType == KeywordType.LET -> {
                ts.next(); false
            }

            else -> false
        }
    }

    fun parseVarDeclStmt(asParam: Boolean = false): VarDeclStmtNode? {
        return ts.captureRange {
            val isMutable = handleVarMutability(asParam = asParam)

            val kind = if (asParam) Terms.PARAM else Terms.VARIABLE
            val name = parseName(kind) ?: return@captureRange null

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

    private fun parseFuncExtensionTypeAndName(): Pair<BaseDatatypeNode?, IdentifierNode>? {
        var name = parseName(kind = Terms.FUNCTION) ?: return null

        val extensionDatatype = if (ts.matchOperator(OperatorType.LESS)) {
            val datatype = parser.parseDatatype(startIdentifier = name)
            ts.expectOperator(OperatorType.DOT, Msg.EXPECTED_DOT)
            datatype
        } else if (ts.matchOperator(OperatorType.DOT)) {
            ts.next()
            name.toDatatype()
        } else null

        if (extensionDatatype != null)
            name = parseName(kind = Terms.FUNCTION) ?: return null

        if (ts.matchOperator(OperatorType.LESS)) {
            val consumedTypeParamsList = parseTemplateList() // eat
            syntaxError(Msg.TYPE_NAMES_MUST_BE_PLACES_BEFORE_FUNC_NAME, consumedTypeParamsList.range)
        }

        return extensionDatatype to name
    }

    fun parseFuncDeclStmt(): FuncDeclStmtNode? {
        return ts.captureRange {
            ts.next()

            val templates = parseTemplateList()

            val pair = parseFuncExtensionTypeAndName()
                ?: return@captureRange null

            val (extensionDatatype, name) = pair

            val params = parseParams()

            val returnType = if (ts.matchOperator(OperatorType.ARROW)) {
                ts.next()
                parser.parseDatatype()
            } else VoidDatatypeNode(range = name.range)

            val isExpressionBodied = ts.matchOperator(OperatorType.ASSIGN)

            val body = if (isExpressionBodied) {
                ts.next()
                parser.parseBlock()
            } else if (ts.match(Token.LBrace::class))
                parseBodyForDeclStmt()
            else
                null

            FuncDeclStmtNode(
                modifiers = null,
                name = name,
                params = params,
                templates = templates,
                returnType = returnType,
                isExpressionBodied = isExpressionBodied,
                extensionDatatype = extensionDatatype,
                body = body,
                range = resultRange
            )
        }
    }

    fun parseConstructorStmt(): ConstructorDeclStmtNode {
        return ts.captureRange {
            ts.next()

            ConstructorDeclStmtNode(
                modifiers = null,
                params = parseParams(),
                body = parseBodyForDeclStmt(),
                range = resultRange
            )
        }
    }

    fun parseDestructorStmt(): DestructorDeclStmtNode {
        return ts.captureRange {
            ts.next()

            parseParams().let {
                if (it.isNotEmpty())
                    syntaxError(Msg.DESTRUCTORS_CANNOT_HAVE_PARAMS, startRange)
            }

            DestructorDeclStmtNode(
                modifiers = null,
                body = parser.parseBlock(),
                range = resultRange
            )
        }
    }

    private fun parseSuperType() =
        if (ts.matchOperator(OperatorType.COLON)) {
            ts.next()
            parser.parseDatatype()
        } else null

    fun parseClassStmt(): ClassDeclStmtNode? {
        return ts.captureRange {
            ts.next()

            val name = parseName(kind = Terms.CLASS) ?: return@captureRange null
            val templates = parseTemplateList()
            val superClass = parseSuperType()
            val body = parseBodyForDeclStmt()

            ClassDeclStmtNode(
                modifiers = null,
                name = name,
                primaryConstrParams = emptyList(),
                templates = templates,
                superClass = superClass,
                body = body,
                range = resultRange
            )
        }
    }

    fun parseInterfaceStmt(): InterfaceDeclStmtNode? {
        return ts.captureRange {
            ts.next()

            val name = parseName(kind = Terms.INTERFACE) ?: return@captureRange null
            val templates = parseTemplateList()
            val superInterface = parseSuperType()
            val body = parseBodyForDeclStmt()

            InterfaceDeclStmtNode(
                modifiers = null,
                name = name,
                templates = templates,
                superInterface = superInterface,
                body = body,
                range = resultRange
            )
        }
    }

    fun parseEnumStmt(): EnumDeclStmtNode? {
        return ts.captureRange {
            ts.next()

            val name = parseName(kind = Terms.ENUM) ?: return@captureRange null

            EnumDeclStmtNode(
                modifiers = null,
                name = name,
                body = parseBodyForDeclStmt(),
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

            val expr = parserFunction()
                ?: if (ts.matchOperator(OperatorType.COMMA))
                    continue
                else break

            list.add(expr)

            if (ts.matchOperator(OperatorType.COMMA))
                ts.next()
        }

        return list
    }

    private fun parseParams(): List<VarDeclStmtNode> =
        parseExprList(
            isOpenToken = { ts.match(Token.LParen::class) },
            isCloseToken = { ts.match(Token.RParen::class) },
        ) { parseVarDeclStmt(asParam = true) }

    private fun parseTemplateList(): TemplateParamsListNode =
        ts.captureRange {
            val list = parseExprList(
                isOpenToken = { ts.matchOperator(OperatorType.LESS) },
                isCloseToken = { ts.matchOperator(OperatorType.GREATER) },
                parserFunction = ::parseTemplateParam
            )

            TemplateParamsListNode(
                params = list,
                range = resultRange
            )
        }

    private fun parseTemplateParam(): TemplateParamNode? =
        ts.captureRange {
            val name = parseName(kind = Terms.TYPE_PARAM) ?: run {
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

    private fun parseName(kind: String): IdentifierNode? {
        return ts.consume(Token.Identifier::class, Msg.FNameExpected.format(kind))
            ?.toIdentifierNode()
    }
}
