package lang.parser

import lang.core.KeywordType
import lang.core.LangSpec
import lang.core.operators.OperatorMaps
import lang.core.operators.OperatorType
import lang.core.SourceRange
import lang.mappers.BinOpTypeMapper
import lang.mappers.UnaryOpTypeMapper
import lang.messages.Msg
import lang.nodes.*
import lang.parser.ParserUtils.flattenCommaNode
import lang.parser.ParserUtils.isBinOperator
import lang.parser.ParserUtils.isKeyword
import lang.parser.ParserUtils.isNotOperator
import lang.parser.ParserUtils.isOperator
import lang.parser.ParserUtils.isSimpleUnaryOp
import lang.parser.ParserUtils.range
import lang.parser.ParserUtils.toIdentifierNode
import lang.parser.ParserUtils.tryConvertToDatatype
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
//        val startsWithLParen = ts.match(Token.LParen::class)

        val list: MutableList<ExprNode> = mutableListOf()

//        if (startsWithLParen)
        ts.next()

        if (!ts.match(Token.RParen::class)) {
            val expr = parse().flattenCommaNode()
            list.addAll(expr)
        }

//        if (startsWithLParen) {
        ts.expect(Token.RParen::class, Msg.EXPECTED_RPAREN)
        ts.next()
//        }

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
        val range = rawBlock.range

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
                    range = SourceRange()
                )
            }
        }

        val lambda = LambdaNode(
            body = block,
            params = params,
            range = range
        )

        return lambda
    }

    private fun parseLambda(): LambdaNode =
        blockToLambda(rawBlock = parser.parseBlock())


    private fun parseDotChain(startChain: ExprNode, ctx: ParsingContext): ExprNode {
        return ts.captureRange {
            var chain: ExprNode = startChain

            while (ts.peek() isOperator OperatorType.DOT) {
                val op = ts.peek() as Token.Operator

                ts.next()
                val expr = when (val t = ts.peek()) {
                    is Token.Identifier -> t.toIdentifierNode()

                    else -> {
                        syntaxError(Msg.EXPECTED_IDENTIFIER, t.range)
                        break
                    }
                }

                ts.next()

                chain = when (op.type) {
                    OperatorType.DOT ->
                        DotAccessNode(
                            base = chain,
                            member = expr,
                            range = resultRange
                        )

                    else -> {
                        syntaxError(Msg.UNEXPECTED_TOKEN, op.range) // unreachable
                        break
                    }
                }

                chain = parsePostfixExpr(ctx = ctx, chain)
            }

            chain
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun parsePrimaryExpr(ctx: ParsingContext): ExprNode {
        // no captureRange required.
        return when (val t = ts.peek()) {
            is Token.Identifier -> {
                ts.next()
                val id = IdentifierNode(value = t.value, range = t.range)
                parseDotChain(startChain = id, ctx = ctx)
            }

            is Token.Int32 -> {
                ts.next(); LiteralNode.IntLiteral(value = t.value, range = t.range)
            }

            is Token.Float32 -> {
                ts.next(); LiteralNode.FloatLiteral(value = t.value, range = t.range)
            }

            is Token.Double64 -> {
                ts.next(); LiteralNode.DoubleLiteral(value = t.value, range = t.range)
            }

            is Token.Int64 -> {
                ts.next(); LiteralNode.LongLiteral(value = t.value, range = t.range)
            }

            is Token.Bool -> {
                ts.next(); LiteralNode.BooleanLiteral(value = t.value, range = t.range)
            }

            is Token.Null -> {
                ts.next(); NullLiteralNode(range = t.range)
            }

            is Token.Str -> {
                ts.next(); LiteralNode.StringLiteral(value = t.value, range = t.range)
            }

            is Token.Character -> {
                ts.next(); LiteralNode.CharLiteral(value = t.value, range = t.range)
            }

            is Token.Keyword -> {
                if (ctx == ParsingContext.FuncHeader)
                    parseOperator()
                else
                    parser.parseStmt()
            }

            is Token.UInt32 -> {
                ts.next(); LiteralNode.UIntLiteral(value = t.value, range = t.range)
            }

            is Token.UInt64 -> {
                ts.next(); LiteralNode.ULongLiteral(value = t.value, range = t.range)
            }

            is Token.LParen -> {
                ts.next()
                val expr = parse()
                if (ts.expect(Token.RParen::class, Msg.EXPECTED_RPAREN))
                    ts.next()


                if (ts.peek() isOperator OperatorType.DOT)
                    parseDotChain(startChain = expr, ctx = ctx)
                else expr
            }

            is Token.LBracket -> parseInitialiserList()
            is Token.LBrace -> parseLambda()

            else -> {
                syntaxError(Msg.EXPECTED_AN_EXPRESSION, ts.prev().range)
                UnknownNode(t.range)
            }
        }
    }

    private fun parsePostfixExpr(ctx: ParsingContext, expr: ExprNode = parsePrimaryExpr(ctx)): ExprNode {
        val t = ts.peek()
        val resultRange = expr.range untilEndOf t.range

        return when (t) {
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
                    range = resultRange
                )
            }

            else -> expr
        }
    }

    private fun parseCallAndIndexChain(expr: ExprNode, ctx: ParsingContext): ExprNode {
        return ts.captureRange {
            var expr2 = expr

            while (true) {
                expr2 = when (ts.peek()) {
                    is Token.LParen -> {
                        val args = parseArgsList(ctx = ctx)

                        FuncCallNode(
                            receiver = expr2,
                            typeNames = null,
                            args = args,
                            range = resultRange
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
                            range = resultRange
                        )
                    }

                    else -> break
                }
            }

            expr2
        }
    }

    private fun parsePostfixOperator(expr: ExprNode, ctx: ParsingContext): ExprNode {
        val t = ts.peek()
        if (t !is Token.Operator) return expr
        val resultRange = expr.range untilEndOf t.range

        return when (t.type) {
            OperatorType.INCREMENT -> {
                ts.next()

                IncrementNode(
                    operand = expr,
                    isPost = true,
                    range = resultRange
                )
            }

            OperatorType.DECREMENT -> {
                ts.next()

                DecrementNode(
                    operand = expr,
                    isPost = true,
                    range = resultRange
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
                        syntaxError(Msg.EXPECTED_TYPE_NAME, resultRange)
                        ErrorDatatypeNode(resultRange)
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
                        syntaxError(Msg.EXPECTED_IDENTIFIER, expr.range)
                        return datatypeNode
                    }

                    return FuncCallNode(
                        receiver = datatypeNode.identifier,
                        typeNames = datatypeNode.typeNames,
                        args = args,
                        range = expr.range
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
                    range = resultRange
                )
            }

            else -> expr
        }
    }

    override fun parseTypenameList(): TypeNameListNode? {
        // require '<'
        if (ts.peek() isNotOperator OperatorType.LESS) {
            syntaxError(Msg.EXPECTED_LESS_OP, ts.range)
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
        val list = ts.captureRange {
            ts.next()

            // handling empty '<>'
            if (ts.peek() isOperator OperatorType.GREATER) {
                ts.next()
                return@captureRange emptyList()
            }

            val list = parseBinaryExpr(1, closeToken, ParsingContext.TypeArg)
                .flattenCommaNode()

            if (ts.peek() isOperator OperatorType.GREATER)
                ts.next()
            else
                syntaxError(Msg.EXPECTED_GREATER_OP, ts.range)

            list
        }

        return analiseTemplateList(list)
    }

    override fun analiseTemplateList(exprList: List<ExprNode>?): TypeNameListNode? {
        if (exprList.isNullOrEmpty())
            return null

        val params: MutableList<TypeNameNode> = mutableListOf()

        exprList.forEach { expr ->
            val typeName: TypeNameNode? = when (expr) {
                is IdentifierNode -> {
                    TypeNameNode(
                        name = expr,
                        bound = null,
                        range = expr.range
                    )
                }

                is BinOpNode -> {
                    if (expr.operator == BinOpType.COLON) {

                        val identifier =
                            if (expr.left is IdentifierNode) expr.left as IdentifierNode
                            else {
                                syntaxError(Msg.EXPECTED_TYPE_PARAM_NAME, expr.range)
                                return@forEach
                            }

                        val datatype = analiseAsDatatype(expr.right, allowAsExpression = false)

                        if (datatype is DatatypeNode)
                            TypeNameNode(
                                name = identifier,
                                bound = datatype,
                                range = expr.range
                            )
                        else null
                    } else {
                        syntaxError(Msg.EXPECTED_TYPE_PARAM_NAME, expr.left.range)
                        null
                    }
                }

                else -> {
                    syntaxError(Msg.EXPECTED_TYPE_PARAM_NAME, expr.range)
                    null
                }
            }

            if (typeName != null)
                params.add(typeName)
        }

        return TypeNameListNode(
            params = params,
            range = exprList.range(defaultEmpty = SourceRange())
        )
    }

    override fun analiseAsDatatype(
        expr: ExprNode,
        allowAsExpression: Boolean
    ): ExprNode? {
        val datatype = expr.tryConvertToDatatype()

        if (datatype == null) {
            if (allowAsExpression) return expr

            syntaxError(Msg.EXPECTED_TYPE_NAME, expr.range)
            return null
        }

        if (datatype !is DatatypeNode) return datatype

        var successful = true

        datatype.typeNames?.params?.forEach { typeName ->
            successful = successful && analiseAsDatatype(typeName.name, true) != null
            if (typeName.bound != null)
                successful = successful && analiseAsDatatype(typeName.bound, true) != null
        }

        return if (successful) datatype else null
    }

    private fun parseOperator(): OperNode {
        ts.next()

        val range = ts.range

        val operator = if (ts.expect(Token.Operator::class, Msg.EXPECTED_OPERATOR)) {
            ts.next() as Token.Operator
        } else null

        return OperNode(
            operatorType = operator?.type ?: OperatorType.UNKNOWN,
            range = range
        )
    }

    private fun parseUnaryExpr(ctx: ParsingContext): ExprNode {
        return ts.captureRange {
            when (val t = ts.peek()) {
                is Token.Semicolon -> {
                    syntaxError(Msg.EXPECTED_AN_EXPRESSION, resultRange)
                    UnknownNode(resultRange)
                }

                is Token.Keyword -> {
                    when (t.type) {
                        KeywordType.CONST -> parseDatatype(startIdentifier = null)
                        KeywordType.FUNC -> parseFuncDatatype()
                        KeywordType.OPERATOR -> parsePostfixExpr(ctx)
                        else -> {
                            syntaxError(Msg.UNEXPECTED_TOKEN, resultRange)
                            UnknownNode(resultRange)
                        }
                    }
                }

                is Token.Operator -> {
                    when {
                        t.type.isSimpleUnaryOp() -> {
                            ts.next()
                            val operator = unaryOpTypeMapper.toSecond(t.type)

                            if (operator == null) {
                                syntaxError(Msg.EXPECTED_AN_EXPRESSION, resultRange)
                                return@captureRange parsePostfixExpr(ctx)
                            }

                            UnaryOpNode(
                                operand = parseUnaryExpr(ctx),
                                operator = operator,
                                tokenOperatorType = t.type,
                                range = resultRange
                            )
                        }

                        t.type == OperatorType.INCREMENT -> {
                            ts.next()

                            IncrementNode(
                                operand = parseUnaryExpr(ctx),
                                isPost = false,
                                range = resultRange
                            )
                        }

                        t.type == OperatorType.DECREMENT -> {
                            ts.next()

                            DecrementNode(
                                operand = parseUnaryExpr(ctx),
                                isPost = false,
                                range = resultRange
                            )
                        }

                        else -> parsePostfixExpr(ctx)
                    }
                }

                else -> parsePostfixExpr(ctx)
            }
        }
    }

    private fun parseInitialiserList(): InitialiserList {
        val range = ts.next().range

        val exprList = parse().flattenCommaNode()

        if (ts.expect(Token.RBracket::class, Msg.EXPECTED_RBRACKET))
            ts.next()

        return InitialiserList(nodes = exprList, range = range)
    }

    private fun parseFuncDatatype(isConst: Boolean = false): BaseDatatypeNode {
        val range = ts.next().range

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
        } else VoidDatatypeNode(ts.range)

        return FuncDatatypeNode(
            paramDatatypes = paramDatatypes,
            returnDatatype = returnDatatype,
            isConst = isConst,
            ptrLvl = ptrLvl,
            isReference = isReference,
            range = range
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
        if (ctx == ParsingContext.Datatype && startDatatype == null && ts.peek() isKeyword KeywordType.FUNC)
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
                syntaxError(Msg.EXPECTED_TYPE_NAME, member.range)
                break
            }

            datatype = ScopedDatatypeNode(
                base = datatype,
                member = member,
                range = member.range
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
        return ts.captureRange {
            if (ts.peek() isKeyword KeywordType.FUNC)
                return@captureRange parseFuncDatatype()

            var isConst = false

            val identifier = if (startIdentifier == null) {
                var t = ts.next()

                isConst = t isKeyword KeywordType.CONST

                if (isConst) t = ts.next()

                when {
                    t is Token.Identifier -> t.toIdentifierNode()
                    t isKeyword KeywordType.FUNC -> return@captureRange parseFuncDatatype(isConst)
                    else -> {
                        syntaxError(Msg.EXPECTED_TYPE_NAME, t.range)
                        ts.next()
                        return@captureRange ErrorDatatypeNode(t.range)
                    }
                }
            } else startIdentifier


            var typeNames: TypeNameListNode? = null


            if (ts.matchOperator(OperatorType.SCOPE)) {
                ts.save()
                ts.next()

                if (ts.matchOperator(OperatorType.LESS)) {
                    ts.clearLastSave()
                } else ts.restore()
            }

            val range = resultRange

            if (ts.matchOperator(OperatorType.LESS)) {
                typeNames = parseTypenameList()
            }

            return@captureRange DatatypeNode(
                identifier = identifier,
                typeNames = typeNames,
                range = range,
                ptrLvl = 0,
                isReference = false,
                isConst = isConst
            )
        }
    }


    fun calcPtrLvl(): Int {
        ts.splitOperators(mapTag = OperatorType.MUL)
        var i = 0
        while (true) {
            val t = ts.peek()
            if (t isNotOperator OperatorType.MUL ||
                !ts.prev().areOnSameLine(t)
            )
                break
            i++; ts.next()
        }
        return i
    }

    fun checkReference(): Boolean {
        var isReference = false
        var hasRedundantAmp = false
        var refPos = ts.range

        ts.splitOperators(mapTag = OperatorType.AMPERSAND)

        while (ts.matchOperator(OperatorType.AMPERSAND)) {
            if (isReference && !hasRedundantAmp) {
                hasRedundantAmp = true
                refPos = ts.range
            } else isReference = true
            ts.next()
        }

        if (hasRedundantAmp)
            syntaxError(Msg.REF_TO_REF_IS_NOT_ALLOWED, refPos)

        // check if pointer to reference (example: int&**)
        if (isReference && calcPtrLvl() != 0) {
            val lastPos = ts.range
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
                    parser.parseBlock()
//                    BlockNode(nodes = emptyList(), range = op.range)
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
        range: SourceRange
    ): ExprNode? {
        val compound = opType.compoundToBinary() ?: return null
        val operator = binOpTypeMapper.toSecond(compound) ?: return null

        val compoundExpr = BinOpNode(
            left = left,
            right = right,
            operator = operator,
            tokenOperatorType = compound,
            range = range
        )

        return BinOpNode(
            left = left,
            right = compoundExpr,
            operator = BinOpType.ASSIGN,
            tokenOperatorType = OperatorType.ASSIGN,
            range = range
        )
    }

    private fun canParseInfix(minPrec: Int): Boolean {
        val t = ts.peek()
        val prec = LangSpec.InfixOperator.precedence
        return prec >= minPrec && ts.prev().areOnSameLine(t)
    }

    private fun parseBinaryExpr(
        minPrec: Int,
        stopToken: Token? = null,
        ctx: ParsingContext = ParsingContext.Default
    ): ExprNode {
        return ts.captureRange {
            var left = parseUnaryExpr(ctx)
//            infix fun Int.a(b: Int) : Int = 1
//            infix fun Int.b(b: Int) : Int = 1
//            fun c() : Int = 1
//
//            1 a c()

            while (true) {
                val t = ts.peek()

                var prec = LangSpec.InfixOperator.precedence

                if (t is Token.Identifier && canParseInfix(minPrec)) {
                    val receiver = t.toIdentifierNode()
                    ts.next()
                    val right = parseBinaryExpr(prec + 1, stopToken, ctx)

                    left = InfixFuncCallNode(
                        receiver = receiver,
                        typeNames = null,
                        args = listOf(left) + right,
                        range = resultRange
                    )
                    continue
                }

                val op = t as? Token.Operator ?: break
                val opType = op.type
                prec = op.precedence

                if (op == stopToken || prec < minPrec) break
                if (
                    ctx.canParseTypeArgs() &&
                    (opType == OperatorType.LESS ||
                            opType == OperatorType.GREATER ||
                            OperatorMaps.triBracketsMap[opType] != null)
                ) break

                if (left isBinOperator BinOpType.COLON &&
                    op.type != OperatorType.COMMA &&
                    op.type != OperatorType.ASSIGN
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
                    range = resultRange
                )

                if (compoundExpr != null) {
                    left = compoundExpr
                    continue
                }


                val operator = binOpTypeMapper.toSecond(opType)

                if (operator == null) {
                    syntaxError(Msg.EXPECTED_AN_EXPRESSION, left.range)
                    break
                }


                left = BinOpNode(
                    left = left,
                    right = right,
                    operator = operator,
                    tokenOperatorType = opType,
                    range = resultRange
                )
            }

            left
        }
    }

    private fun syntaxError(msg: String, range: SourceRange) =
        parser.syntaxError(msg = msg, range = range)

    private fun warning(msg: String, range: SourceRange) =
        parser.warning(msg = msg, range = range)
}