package lang.parser

import lang.mappers.BinOpTypeMapper
import lang.mappers.UnaryOpTypeMapper
import lang.messages.Msg
import lang.nodes.*
import lang.parser.ParserUtils.flattenCommaNode
import lang.parser.ParserUtils.isKeyword
import lang.parser.ParserUtils.isNotOperator
import lang.parser.ParserUtils.isOperator
import lang.parser.ParserUtils.isSimpleUnaryOp
import lang.parser.ParserUtils.toIdentifierNode
import lang.tokens.*

class ExprParser(
    private val ts: ITokenStream,
    private val parser: IParser,
) : IExprParser {
    private val unaryOpTypeMapper = UnaryOpTypeMapper()
    private val binOpTypeMapper = BinOpTypeMapper()

    override fun parse(ctx: ParsingContext): ExprNode {
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

        ts.expect(Token.RParen::class, Msg.EXPECTED_RPAREN)
        ts.next()

        if (ctx == ParsingContext.Default && ts.match(Token.LBrace::class)) {
            parseLambda().let { lambda ->
                list.add(lambda)
            }
        }

        return list.toList()
    }

    /*private fun hasLambdaSignature(rawBlock: BlockNode): Boolean {
        val first = rawBlock.nodes.firstOrNull() ?: return false
        return first isBinOperator BinOpType.ARROW
    }*/

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

        while (ts.peek() isOperator OperatorType.DOT) {
            val op = ts.peek() as Token.Operator

            ts.next()
            val expr = when (val t = ts.peek()) {
                is Token.Identifier -> t.toIdentifierNode()

                else -> {
                    syntaxError(Msg.EXPECTED_IDENTIFIER, t.pos)
                    break
                }
            }

            ts.next()

            chain = when (op.type) {
                OperatorType.DOT ->
                    DotAccessNode(
                        base = chain,
                        member = expr,
                        pos = op.pos
                    )

                else -> {
                    syntaxError(Msg.UNEXPECTED_TOKEN, op.pos) // unreachable
                    break
                }
            }

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
                ts.next(); LiteralNode.IntLiteral(value = t.value, pos = t.pos)
            }

            is Token.Float32 -> {
                ts.next(); LiteralNode.FloatLiteral(value = t.value, pos = t.pos)
            }

            is Token.Double64 -> {
                ts.next(); LiteralNode.DoubleLiteral(value = t.value, pos = t.pos)
            }

            is Token.Int64 -> {
                ts.next(); LiteralNode.LongLiteral(value = t.value, pos = t.pos)
            }

            is Token.Bool -> {
                ts.next(); LiteralNode.BooleanLiteral(value = t.value, pos = t.pos)
            }

            is Token.Null -> {
                ts.next(); NullLiteralNode(pos = t.pos)
            }

            is Token.Str -> {
                ts.next(); LiteralNode.StringLiteral(value = t.value, pos = t.pos)
            }

            is Token.Character -> {
                ts.next(); LiteralNode.CharLiteral(value = t.value, pos = t.pos)
            }

            is Token.Keyword -> {
                if (ctx == ParsingContext.FuncHeader)
                    parseOperator()
                else
                    parser.parseStmt()
            }

            is Token.UInt32 -> {
                ts.next(); LiteralNode.UIntLiteral(value = t.value, pos = t.pos)
            }

            is Token.UInt64 -> {
                ts.next(); LiteralNode.ULongLiteral(value = t.value, pos = t.pos)
            }

            is Token.LParen -> {
                ts.next()
                val expr = parse()
                if (ts.expect(Token.RParen::class, Msg.EXPECTED_RPAREN))
                    ts.next()


                if (ts.peek() isOperator OperatorType.DOT)
                    parseDotChain(startChain = expr)
                else expr
            }

            is Token.LBracket -> parseInitialiserList()
            is Token.LBrace -> parseLambda()

            else -> {
                syntaxError(Msg.EXPECTED_AN_EXPRESSION, ts.prev().pos)
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
                    receiver = expr,
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
                        receiver = expr2,
                        typeNames = null,
                        args = args,
                        pos = t.pos
                    )
                }

                is Token.LBracket -> {
                    ts.next()
                    val indexExpr = parse()
                    ts.expect(Token.RBracket::class, Msg.EXPECTED_RBRACKET)
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
                parseDatatype(startIdentifier = expr)
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

            OperatorType.SCOPE -> {
                when (expr) {
                    is IdentifierNode -> {
                        val expr2 = parseDatatype(startIdentifier = expr, ctx = ParsingContext.ScopeChain)
                        parsePostfixExpr(ctx, expr2)
                    }

                    is QualifiedDatatypeNode -> {
                        val expr2 = parseDatatype(startDatatype = expr, ctx = ParsingContext.ScopeChain)
                        parsePostfixExpr(ctx, expr2)
                    }

                    else -> {
                        syntaxError(Msg.EXPECTED_TYPE_NAME, t.pos)
                        ErrorDatatypeNode(t.pos)
                    }
                }
            }

            OperatorType.LESS -> {
//                if (!ctx || expr !is IdentifierNode)
                if (!ctx.canParseTypeArgs() || expr !is IdentifierNode)
                    return expr

                val datatypeNode = parseDatatype(startIdentifier = expr)

                if (ts.match(Token.LParen::class)) {
                    val args = parseArgsList(ctx = ctx)

                    if (datatypeNode !is DatatypeNode) {
                        syntaxError(Msg.EXPECTED_IDENTIFIER, expr.pos)
                        return datatypeNode
                    }

                    return FuncCallNode(
                        receiver = datatypeNode.identifier,
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
            syntaxError(Msg.EXPECTED_LESS_OP, ts.pos)
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

        val list = parseBinaryExpr(1, closeToken, ParsingContext.TypeArg).flattenCommaNode()

        if (ts.peek() isOperator OperatorType.GREATER)
            ts.next()
        else
            syntaxError(Msg.EXPECTED_GREATER_OP, ts.pos)

        return list
    }

    private fun parseOperator(): OperNode {
        ts.next()

        val pos = ts.pos

        val operator = if (ts.expect(Token.Operator::class, Msg.EXPECTED_OPERATOR)) {
            ts.next() as Token.Operator
        } else null

        return OperNode(
            operatorType = operator?.type ?: OperatorType.UNKNOWN,
            pos = pos
        )
    }

    private fun parseUnaryExpr(ctx: ParsingContext): ExprNode {
        return when (val t = ts.peek()) {
            is Token.Semicolon -> {
                syntaxError(Msg.EXPECTED_AN_EXPRESSION, t.pos)
                UnknownNode(t.pos)
            }

            is Token.Keyword -> {
                when (t.type) {
                    KeywordType.CONST -> parseDatatype(startIdentifier = null)
                    KeywordType.FUNC -> parseFuncDatatype()
                    KeywordType.OPERATOR -> parsePostfixExpr(ctx)
                    else -> {
                        syntaxError(Msg.UNEXPECTED_TOKEN, t.pos)
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
                            syntaxError(Msg.EXPECTED_AN_EXPRESSION, t.pos)
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

        if (ts.expect(Token.RBracket::class, Msg.EXPECTED_RBRACKET))
            ts.next()

        return InitialiserList(nodes = exprList, pos = pos)
    }

    private fun parseFuncDatatype(isConst: Boolean = false): BaseDatatypeNode {
        val pos = ts.next().pos

        if (ts.matchOperator(OperatorType.MUL))
            ts.next() // skip first '*'. (level 1 ptr is also func type)

        val ptrLvl = calcPtrLvl()
        val isReference = checkReference()

        val paramDatatypes = if (ts.match(Token.LParen::class)) {
            val rawParamTypes = parseArgsList(ctx = ParsingContext.Header)
            parser.analiseDatatypeList(rawParamTypes) ?: emptyList()
        } else emptyList()

        val returnDatatype = if (ts.matchOperator(OperatorType.COLON)) {
            ts.next()
            parseDatatype(startIdentifier = null)
        } else VoidDatatypeNode(ts.pos)

        return FuncDatatypeNode(
            paramDatatypes = paramDatatypes,
            returnDatatype = returnDatatype,
            isConst = isConst,
            ptrLvl = ptrLvl,
            isReference = isReference,
            pos = pos
        )
    }

    private fun parseDatatype(
        startIdentifier: IdentifierNode? = null,
        ctx: ParsingContext = ParsingContext.Datatype
    ): BaseDatatypeNode {
        val datatype = parseSimpleDatatype(startIdentifier = startIdentifier)
        return parseDatatype(startDatatype = datatype, ctx = ctx)
    }

    private fun parseDatatype(
        startDatatype: BaseDatatypeNode? = null,
        ctx: ParsingContext = ParsingContext.Datatype
    ): BaseDatatypeNode {
        if (ts.peek() isKeyword KeywordType.FUNC)
            return parseFuncDatatype()

        var datatype = startDatatype ?: parseSimpleDatatype(startIdentifier = null)
        var memberDatatype = datatype

        while (
            datatype is QualifiedDatatypeNode &&
            ts.matchOperator(OperatorType.SCOPE)
        ) {
            ts.next()
            val member = parseSimpleDatatype()

            if (member !is DatatypeNode) {
                syntaxError(Msg.EXPECTED_TYPE_NAME, member.pos)
                break
            }

            datatype = ScopedDatatypeNode(
                base = datatype,
                member = member,
                pos = member.pos
            )

            memberDatatype = member
        }

        if (memberDatatype is DatatypeNode && ctx is ParsingContext.Datatype) {
            memberDatatype.ptrLvl = calcPtrLvl()
            memberDatatype.isReference = checkReference()
        }

        return datatype
    }


    private fun parseSimpleDatatype(startIdentifier: IdentifierNode? = null): BaseDatatypeNode {
        if (ts.peek() isKeyword KeywordType.FUNC)
            return parseFuncDatatype()

        var isConst = false

        val identifier = if (startIdentifier == null) {
            var t = ts.next()

            isConst = t isKeyword KeywordType.CONST

            if (isConst) t = ts.next()

            when {
                t is Token.Identifier -> t.toIdentifierNode()
                t isKeyword KeywordType.FUNC -> return parseFuncDatatype(isConst)
                else -> {
                    syntaxError(Msg.EXPECTED_TYPE_NAME, t.pos)
                    ts.next()
                    return ErrorDatatypeNode(t.pos)
                }
            }
        } else startIdentifier

        val pos = identifier.pos

        var typeNames: List<ExprNode>? = null


        if (ts.matchOperator(OperatorType.SCOPE)) {
            ts.save()
            ts.next()

            if (ts.matchOperator(OperatorType.LESS)) {
                ts.clearLastSave()
            } else ts.restore()
        }

        if (ts.matchOperator(OperatorType.LESS)) {
            typeNames = parseTypenameList()

            if (typeNames == null) {
                syntaxError(Msg.EXPECTED_TYPE_NAME, pos)
                return ErrorDatatypeNode(pos)
            }
        }

        return DatatypeNode(
            identifier = identifier,
            typeNames = typeNames,
            pos = pos,
            ptrLvl = 0,
            isReference = false,
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
        var refPos = ts.pos

        ts.splitOperators(mapTag = OperatorType.AMPERSAND)

        while (ts.matchOperator(OperatorType.AMPERSAND)) {
            if (isReference && !hasRedundantAmp) {
                hasRedundantAmp = true
                refPos = ts.pos
            } else isReference = true
            ts.next()
        }

        if (hasRedundantAmp)
            syntaxError(Msg.REF_TO_REF_IS_NOT_ALLOWED, refPos)

        // check if pointer to reference (example: int&**)
        if (isReference && calcPtrLvl() != 0) {
            val lastPos = ts.pos
            syntaxError(Msg.POINTER_TO_REFERENCE_IS_NOT_ALLOWED, lastPos)
        }

        return isReference
    }

    private fun parseRight(
        op: Token.Operator,
        stopToken: Token?,
        ctx: ParsingContext
    ): ExprNode {
        return when (op.type) {
            OperatorType.ASSIGN -> parser.parseStmt(isSingleLine = true)

            OperatorType.COLON, OperatorType.AS -> parseDatatype(startIdentifier = null)

            OperatorType.ARROW -> {
                if (ts.match(Token.RBrace::class))
                    BlockNode(nodes = emptyList(), pos = op.pos)
                else
                    parser.parseStmt(isSingleLine = true)
            }

            else -> parseBinaryExpr(op.precedence + 1, stopToken, ctx)
        }
    }

    private fun splitCompound(
        left: ExprNode,
        right: ExprNode,
        opType: OperatorType,
        pos: Pos
    ): ExprNode? {
        val compound = opType.compoundToBinary() ?: return null
        val operator = binOpTypeMapper.toSecond(compound) ?: return null

        val compoundExpr = BinOpNode(
            left = left,
            right = right,
            operator = operator,
            tokenOperatorType = compound,
            pos = pos
        )

        return BinOpNode(
            left = left,
            right = compoundExpr,
            operator = BinOpType.ASSIGN,
            tokenOperatorType = OperatorType.ASSIGN,
            pos = pos
        )
    }

    private fun parseBinaryExpr(
        minPrec: Int,
        stopToken: Token? = null,
        ctx: ParsingContext = ParsingContext.Default
    ): ExprNode {
        var left = parseUnaryExpr(ctx)

        while (true) {
            val op = ts.peek() as? Token.Operator ?: break
            val opType = op.type

            if (op == stopToken || op.precedence < minPrec) break
            if (
                ctx.canParseTypeArgs() &&
                (opType == OperatorType.LESS ||
                        opType == OperatorType.GREATER ||
                        OperatorMaps.triBracketsMap[opType] != null)
            ) break

            ts.next()

            val right = parseRight(
                op = op,
                stopToken = stopToken,
                ctx = ctx
            )

            val compoundExpr = splitCompound(
                left = left,
                right = right,
                opType = opType,
                pos = op.pos
            )

            if (compoundExpr != null) {
                left = compoundExpr
                continue
            }


            val operator = binOpTypeMapper.toSecond(opType)

            if (operator == null) {
                syntaxError(Msg.EXPECTED_AN_EXPRESSION, left.pos)
                break
            }

            left = BinOpNode(
                left = left,
                right = right,
                operator = operator,
                tokenOperatorType = opType,
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