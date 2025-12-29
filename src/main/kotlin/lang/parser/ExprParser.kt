package lang.parser

import lang.tokens.KeywordType
import lang.messages.Messages
import lang.tokens.OperatorType
import lang.tokens.ITokenStream
import lang.tokens.Pos
import lang.tokens.Token
import lang.mappers.BinOpTypeMapper
import lang.mappers.UnaryOpTypeMapper
import lang.nodes.BaseDatatypeNode
import lang.nodes.BinOpNode
import lang.nodes.BinOpType
import lang.nodes.BlockNode
import lang.nodes.DatatypeNode
import lang.nodes.DecrementNode
import lang.nodes.ErrorDatatypeNode
import lang.nodes.ExprNode
import lang.nodes.FuncCallNode
import lang.nodes.FuncDatatypeNode
import lang.nodes.IdentifierNode
import lang.nodes.IncrementNode
import lang.nodes.IndexAccessNode
import lang.nodes.InitialiserList
import lang.nodes.LambdaNode
import lang.nodes.LiteralNode
import lang.nodes.MemberAccessNode
import lang.nodes.NullLiteralNode
import lang.nodes.OperNode
import lang.nodes.UnaryOpNode
import lang.nodes.UnknownNode
import lang.nodes.VarDeclStmtNode
import lang.nodes.VoidDatatypeNode
import lang.parser.ParserUtils.flattenCommaNode
import lang.parser.ParserUtils.isAccessOperator
import lang.parser.ParserUtils.isBinOperator
import lang.parser.ParserUtils.isKeyword
import lang.parser.ParserUtils.isNotOperator
import lang.parser.ParserUtils.isOperator
import lang.parser.ParserUtils.isSimpleUnaryOp
import lang.parser.ParserUtils.toIdentifierNode

class ExprParser(
    private val ts: ITokenStream,
    private val parser: IParser,
) : IExprParser {
    private val unaryOpTypeMapper = UnaryOpTypeMapper()
    private val binOpTypeMapper = BinOpTypeMapper()

    private var parseCallCount = 0

    override fun parse(ctx: ParsingContext): ExprNode {
//        if (++parseCallCount % 1000 == 0) {
//            true
//        }
        ts.skipTokens(Token.Semicolon::class)
        return parseBinaryExpr(minPrec = 1, null, ctx)
    }

    override fun parseArgsList(ctx: ParsingContext): List<ExprNode> {
        val list: MutableList<ExprNode> = mutableListOf()
        ts.next()

        if (!ts.match(Token.RParen::class)) {
            val expr = parse().flattenCommaNode()
            list.addAll(expr)
        }

        ts.expect(Token.RParen::class, Messages.EXPECTED_RPAREN)
        ts.next()

        if (ctx == ParsingContext.Default && ts.match(Token.LBrace::class)) {
            parseLambda().let { lambda ->
                list.add(lambda)
            }
        }

        return list.toList()
    }

    private fun hasLambdaSignature(rawBlock: BlockNode): Boolean {
        val first = rawBlock.nodes.firstOrNull() ?: return false
        return first isBinOperator BinOpType.ARROW
    }

    private fun blockToLambda(rawBlock: BlockNode): LambdaNode {
        val pos = rawBlock.pos

        var params: List<VarDeclStmtNode>? = null

        var block = rawBlock

        if (block.nodes.isNotEmpty()) {
            val first = block.nodes.first()

            if (first is BinOpNode && first.operator == BinOpType.ARROW) {
                val rawParams = first.left.flattenCommaNode()

                if (rawParams.isNotEmpty())
                    params = parser.analiseParams(rawParams)

                val newList = block.nodes.toMutableList().apply {
                    removeAt(0)
                    add(0, first.right)
                }

                block = BlockNode(
                    nodes = newList,
                    pos = Pos()
                )
            }
        }

        val lambda = LambdaNode(
            body = block,
            params = params,
            pos = pos
        )

        return lambda
    }

    private fun parseLambda(): LambdaNode =
        blockToLambda(rawBlock = parser.parseBlock())


    private fun parseDotChain(startChain: ExprNode): ExprNode {
        var chain: ExprNode = startChain
//        var right: IdentifierNode? = null


        while (ts.peek().isAccessOperator()) {
            val op = ts.peek() as Token.Operator
            val isNullSafe = when (op.type) {
                OperatorType.DOT -> false
                OperatorType.DOT_NULL_SAFE -> true
                else -> break
            }

            ts.next()
            val expr = when (val t = ts.peek()) {
                is Token.Identifier -> {
                    t.toIdentifierNode()
                }
//                is Token.LParen -> parse(ParsingContext.Default)
                else -> {
                    syntaxError(Messages.EXPECTED_IDENTIFIER, t.pos)
                    break
                }
            }

            ts.next()

            chain = MemberAccessNode(
                base = chain,
                member = expr,
                isNullSafe = isNullSafe,
                pos = op.pos
            )

            chain = parsePostfixExpr(ParsingContext.Default, chain)
        }

        return chain
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun parsePrimaryExpr(ctx: ParsingContext): ExprNode {
        return when (val t = ts.peek()) {
            is Token.Identifier -> {
                ts.next()
                val id = IdentifierNode(value = t.value, pos = t.pos)
                parseDotChain(startChain = id)
            }

            is Token.Int32 -> {
                ts.next(); LiteralNode(value = t.value, pos = t.pos)
            }

            is Token.Float32 -> {
                ts.next(); LiteralNode(value = t.value, pos = t.pos)
            }

            is Token.Double64 -> {
                ts.next(); LiteralNode(value = t.value, pos = t.pos)
            }

            is Token.Int64 -> {
                ts.next(); LiteralNode(value = t.value, pos = t.pos)
            }

            is Token.Bool -> {
                ts.next(); LiteralNode(value = t.value, pos = t.pos)
            }

            is Token.Null -> {
                ts.next(); NullLiteralNode(pos = t.pos)
            }

            is Token.QuotesStr -> {
                ts.next(); LiteralNode(value = t.value, pos = t.pos)
            }

            is Token.QuotesChar -> {
                ts.next(); LiteralNode(value = t.value, pos = t.pos)
            }

            is Token.Keyword -> {
                if (ctx == ParsingContext.FuncHeader)
                    parseOperator()
                else
                    parser.parseStmt()
            }

            is Token.UInt32 -> {
                ts.next(); LiteralNode(value = t.value, pos = t.pos)
            }

            is Token.UInt64 -> {
                ts.next(); LiteralNode(value = t.value, pos = t.pos)
            }

            is Token.LParen -> {
                ts.next()
                val expr = parse()
                if (ts.expect(Token.RParen::class, Messages.EXPECTED_RPAREN))
                    ts.next()


                if (ts.peek().isAccessOperator())
                    parseDotChain(startChain = expr)
                else expr
            }

            is Token.LBracket -> parseInitialiserList()
            is Token.LBrace -> parseLambda()

            else -> {
                syntaxError(Messages.EXPECTED_AN_EXPRESSION, ts.prev().pos)
                UnknownNode(t.pos)
            }
        }
    }

    private fun parsePostfixExpr(ctx: ParsingContext, expr: ExprNode = parsePrimaryExpr(ctx)): ExprNode {
        return when (val t = ts.peek()) {
            is Token.Operator ->
                parsePostfixOperator(
                    expr = expr,
                    ctx = ctx
                )

            is Token.LParen,
            is Token.LBracket -> parseCallAndIndexChain(expr, ctx)

            is Token.LBrace -> {
                if (expr !is IdentifierNode || !ctx.canParseTrailingLambdas())
                    return expr

                FuncCallNode(
                    name = expr,
                    typeNames = null,
                    args = listOf(parseLambda()),
                    pos = t.pos
                )
            }

            else -> expr
        }
    }

    private fun parseCallAndIndexChain(expr: ExprNode, ctx: ParsingContext): ExprNode {
        var expr2 = expr

        while (true) {
            expr2 = when (val t = ts.peek()) {
                is Token.LParen -> {
                    val args = parseArgsList(ctx = ctx)

                    FuncCallNode(
                        name = expr2,
                        typeNames = null,
                        args = args,
                        pos = t.pos
                    )
                }

                is Token.LBracket -> {
                    ts.next()
                    val indexExpr = parse()
                    ts.expect(Token.RBracket::class, Messages.EXPECTED_RBRACKET)
                    ts.next()

                    IndexAccessNode(
                        target = expr2,
                        indexExpr = indexExpr,
                        pos = t.pos
                    )
                }

                else -> break
            }
        }
        return expr2
    }

    private fun parsePostfixOperator(expr: ExprNode, ctx: ParsingContext): ExprNode {
        val t = ts.peek()
        if (t !is Token.Operator) return expr

        return when (t.type) {
            OperatorType.INCREMENT -> {
                ts.next()

                IncrementNode(
                    operand = expr,
                    isPost = true,
                    pos = t.pos
                )
            }

            OperatorType.DECREMENT -> {
                ts.next()

                DecrementNode(
                    operand = expr,
                    isPost = true,
                    pos = t.pos
                )
            }

            OperatorType.AMPERSAND -> {
                if (expr !is IdentifierNode) return expr
                return parseDatatype(startIdentifier = expr)
            }

            OperatorType.MUL -> {
                if (expr !is IdentifierNode) return expr

                ts.save()
                ts.next()

                val isPointer = when {
                    ts.match(Token.LParen::class) -> false
                    ts.matchOperator(
                        OperatorType.COMMA,
                        OperatorType.AMPERSAND,
                        OperatorType.AND,
                        OperatorType.BIN_AND_ASSIGN,
                        OperatorType.MUL,
                        OperatorType.MUL_ASSIGN
                    ) -> true

                    else -> false
                }

                ts.restore()

                if (isPointer)
                    return parseDatatype(startIdentifier = expr)

                expr
            }

            OperatorType.LESS -> {
//                if (!ctx || expr !is IdentifierNode)
                if (ctx == ParsingContext.Condition || expr !is IdentifierNode)
                    return expr

                val datatypeNode = parseDatatype(startIdentifier = expr)

                if (ts.match(Token.LParen::class)) {
                    val args = parseArgsList(ctx = ctx)

                    if (datatypeNode !is DatatypeNode) {
                        syntaxError(Messages.EXPECTED_IDENTIFIER, expr.pos)
                        return datatypeNode
                    }

                    return FuncCallNode(
                        name = datatypeNode.identifier,
                        typeNames = datatypeNode.typeNames,
                        args = args,
                        pos = expr.pos
                    )
                }

                datatypeNode
            }

            // postfix unary
            OperatorType.NON_NULL_ASSERT -> {
                val operator = unaryOpTypeMapper.toSecond(t.type) ?: return expr

                ts.next()

                UnaryOpNode(
                    operand = expr,
                    operator = operator,
                    tokenOperatorType = t.type,
                    pos = t.pos
                )
            }

            else -> expr
        }
    }

    override fun parseTypenameList(): List<ExprNode>? {
        // require '<'
        if (ts.peek() isNotOperator OperatorType.LESS) {
            syntaxError(Messages.EXPECTED_LESS_OP, ts.peek().pos)
            return null
        }

        // finding last '>', and splitting (<<, >>, <<=, >>=, <=, >=)

        // array[<]array<int>[>]
        // first-^      last -^
        val closeToken = ts.getEnclosedTriBracketsEndToken()

        // if '>' not found (failed to parse typename list -> it's default expression)
        if (closeToken isNotOperator OperatorType.GREATER)
            return null

        // step inside of <...>
        ts.next()

        // handling empty '<>'
        if (ts.peek() isOperator OperatorType.GREATER) {
            ts.next()
            return emptyList()
        }

        val list = parseBinaryExpr(1, closeToken).flattenCommaNode()

        if (ts.peek() isOperator OperatorType.GREATER)
            ts.next()

        return list
    }

    private fun parseOperator(): OperNode {
        ts.next()

        val pos = ts.peek().pos

        val operator = if (ts.expect(Token.Operator::class, Messages.EXPECTED_OPERATOR)) {
            ts.next() as Token.Operator
        } else null

        return OperNode(
            type = operator?.type ?: OperatorType.UNKNOWN,
            pos = pos
        )
    }

    private fun parseUnaryExpr(ctx: ParsingContext): ExprNode {
        return when (val t = ts.peek()) {
            is Token.Semicolon -> {
                syntaxError(Messages.EXPECTED_AN_EXPRESSION, t.pos)
                UnknownNode(t.pos)
            }

            is Token.Keyword -> {
                when (t.type) {
                    KeywordType.CONST -> parseDatatype()
                    KeywordType.FUNC -> parseFuncDatatype()
                    KeywordType.OPERATOR -> parsePostfixExpr(ctx)
                    else -> {
                        syntaxError(Messages.UNEXPECTED_TOKEN, t.pos)
                        UnknownNode(t.pos)
                    }
                }
            }

            is Token.Operator -> {
                when {
                    t.type.isSimpleUnaryOp() -> {
                        ts.next()
                        val operator = unaryOpTypeMapper.toSecond(t.type)

                        if (operator == null) {
                            syntaxError(Messages.EXPECTED_AN_EXPRESSION, t.pos)
                            return parsePostfixExpr(ctx)
                        }

                        UnaryOpNode(
                            operand = parseUnaryExpr(ctx),
                            operator = operator,
                            tokenOperatorType = t.type,
                            pos = t.pos
                        )
                    }

                    t.type == OperatorType.INCREMENT -> {
                        ts.next()

                        IncrementNode(
                            operand = parseUnaryExpr(ctx),
                            isPost = false,
                            pos = t.pos
                        )
                    }

                    t.type == OperatorType.DECREMENT -> {
                        ts.next()

                        DecrementNode(
                            operand = parseUnaryExpr(ctx),
                            isPost = false,
                            pos = t.pos
                        )
                    }

                    else -> parsePostfixExpr(ctx)
                }
            }

            else -> parsePostfixExpr(ctx)
        }
    }

    private fun parseInitialiserList(): InitialiserList {
        val pos = ts.next().pos

        val exprList = parse().flattenCommaNode()

        if (ts.expect(Token.RBracket::class, Messages.EXPECTED_RBRACKET))
            ts.next()

        return InitialiserList(nodes = exprList, pos = pos)
    }

    private fun parseFuncDatatype(isConst: Boolean = false): BaseDatatypeNode {
        val pos = ts.next().pos

        val ptrLvl = calcPtrLvl()
        val isReference = checkReference()

        val paramDatatypes = if (ts.match(Token.LParen::class)) {
            val rawParamTypes = parseArgsList(ctx = ParsingContext.Header)
            parser.analiseDatatypeList(rawParamTypes) ?: emptyList()
        } else emptyList()

        val returnDatatype = if (ts.matchOperator(OperatorType.COLON)) {
            ts.next()
            parseDatatype()
        } else VoidDatatypeNode(ts.peek().pos)

        return FuncDatatypeNode(
            paramDatatypes = paramDatatypes,
            returnDatatype = returnDatatype,
            isConst = isConst,
            ptrLvl = ptrLvl,
            isReference = isReference,
            pos = pos
        )
    }

    private fun parseDatatype(startIdentifier: IdentifierNode? = null): BaseDatatypeNode {
        if (ts.peek() isKeyword KeywordType.FUNC)
            return parseFuncDatatype()

        var isConst = false

        val identifier = if (startIdentifier == null) {
            var t = ts.next()

            isConst = t isKeyword KeywordType.CONST

            if (isConst)
                t = ts.next()

            when {
                t is Token.Identifier -> t.toIdentifierNode()
                t isKeyword KeywordType.FUNC -> return parseFuncDatatype(isConst)
                else -> {
                    syntaxError(Messages.EXPECTED_TYPE_NAME, t.pos)
                    ts.next()
                    return ErrorDatatypeNode(t.pos)
                }
            }
        } else startIdentifier

        val pos = identifier.pos

        val typeNames =
            if (ts.matchOperator(OperatorType.LESS)) {
                val typeNames = parseTypenameList()
                if (typeNames != null)
                    typeNames
                else {
                    syntaxError(Messages.EXPECTED_TYPE_NAME, pos)
                    return ErrorDatatypeNode(pos)
                }
            } else null


        val ptrLvl = calcPtrLvl()
        val isReference = checkReference()

        return DatatypeNode(
            identifier = identifier,
            typeNames = typeNames,
            pos = pos,
            ptrLvl = ptrLvl,
            isReference = isReference,
            isConst = isConst
        )
    }


    fun calcPtrLvl(): Int {
        ts.splitOperators(mapTag = OperatorType.MUL)
        var i = 0
        while (ts.matchOperator(OperatorType.MUL)) {
            i++; ts.next()
        }
        return i
    }

    fun checkReference(): Boolean {
        var isReference = false
        var hasRedundantAmp = false
        var refPos = ts.peek().pos

        ts.splitOperators(mapTag = OperatorType.AMPERSAND)

        while (ts.matchOperator(OperatorType.AMPERSAND)) {
            if (isReference && !hasRedundantAmp) {
                hasRedundantAmp = true
                refPos = ts.peek().pos
            } else isReference = true
            ts.next()
        }

        if (hasRedundantAmp)
            syntaxError(Messages.REF_TO_REF_IS_NOT_ALLOWED, refPos)

        // check if pointer to reference (example: int&**)
        if (isReference && calcPtrLvl() != 0) {
            val lastPos = ts.peek().pos
            syntaxError(Messages.POINTER_TO_REFERENCE_IS_NOT_ALLOWED, lastPos)
        }

        return isReference
    }

    private fun parseRight(
        left: ExprNode,
        op: Token.Operator,
        stopToken: Token?,
        ctx: ParsingContext
    ): ExprNode {
        return when (op.type) {
            OperatorType.ASSIGN -> parser.parseStmt(isSingleLine = true)

            OperatorType.COLON, OperatorType.AS -> parseDatatype()

            OperatorType.ARROW -> {
                if (ts.match(Token.RBrace::class))
                    BlockNode(nodes = emptyList(), pos = op.pos)
                else
                    parser.parseStmt(isSingleLine = true)
            }

            else -> parseBinaryExpr(op.precedence + 1, stopToken, ctx)
        }
    }

    private fun parseBinaryExpr(
        minPrec: Int,
        stopToken: Token? = null,
        ctx: ParsingContext = ParsingContext.Default
    ): ExprNode {
        var left = parseUnaryExpr(ctx)

        while (true) {
            val op = ts.peek() as? Token.Operator ?: break
            if (op == stopToken || op.precedence < minPrec) break
            ts.next()

            val right = parseRight(
                left = left,
                op = op,
                stopToken = stopToken,
                ctx = ctx
            )

            val operator = binOpTypeMapper.toSecond(op.type)

            if (operator == null) {
                syntaxError(Messages.EXPECTED_AN_EXPRESSION, left.pos)
                break
            }

            left = BinOpNode(
                left = left,
                right = right,
                operator = operator,
                tokenOperatorType = op.type,
                pos = op.pos
            )
        }

        return left
    }

    private fun syntaxError(msg: String, pos: Pos) =
        parser.syntaxError(msg = msg, pos = pos)

    private fun warning(msg: String, pos: Pos) =
        parser.warning(msg = msg, pos = pos)
}