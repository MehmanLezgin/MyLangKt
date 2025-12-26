package lang.tokens

import lang.messages.ErrorHandler
import lang.core.ILangSpec
import lang.lexer.ILexer


class TokenStream(
    private val lexer: ILexer,
    private val langSpec: ILangSpec,
    errorHandler: ErrorHandler
) : BaseTokenStream(
    lexer = lexer,
    errorHandler = errorHandler
) {
    private var tokens: MutableList<Token> = mutableListOf()
    private var index: Int = 0
    private val savedIndices: ArrayDeque<Int> = ArrayDeque()

    init {
        reset()
    }

    override fun getTokens() = tokens.toList()

    override fun reset() {
        lexer.reset()
        tokens = lexer.tokenizeAll().toMutableList()
        index = 0
        savedIndices.clear()
    }

    override fun prev() =
        if (index > 0) tokens[index - 1]
        else Token.EOF(Pos())

    override fun peek() = atIndex(index)

    override fun next() = peek().also { index++ }

    override fun next222(): Token {
        return eof
    }

    private fun atIndex(idx: Int) =
        if (idx in tokens.indices) tokens[idx]
        else eof

    override fun save() {
        super.save()
        savedIndices.add(index)
    }

    override fun restore() {
        super.save()
        if (savedIndices.isEmpty()) return
        index = savedIndices.removeLast()
    }

    override fun clearLastSave() {
        super.save()
        savedIndices.removeLast()
    }


    private val triBracketsMap = mapOf(
        OperatorType.LESS_EQUAL to listOf(OperatorType.LESS, OperatorType.ASSIGN),
        OperatorType.GREATER_EQUAL to listOf(OperatorType.GREATER, OperatorType.ASSIGN),

        OperatorType.SHIFT_LEFT to listOf(OperatorType.LESS, OperatorType.LESS),
        OperatorType.SHIFT_LEFT_ASSIGN to listOf(OperatorType.LESS, OperatorType.LESS, OperatorType.ASSIGN),

        OperatorType.SHIFT_RIGHT to listOf(OperatorType.GREATER, OperatorType.GREATER),
        OperatorType.SHIFT_RIGHT_ASSIGN to listOf(OperatorType.GREATER, OperatorType.GREATER, OperatorType.ASSIGN),
    )

    private val ampersandMap = mapOf(
        OperatorType.AMPERSAND to listOf(OperatorType.AMPERSAND),
        OperatorType.AND to listOf(OperatorType.AMPERSAND, OperatorType.AMPERSAND),
        OperatorType.BIN_AND_ASSIGN to listOf(OperatorType.AMPERSAND, OperatorType.ASSIGN)
    )

    private val multiplyMap = mapOf(
        OperatorType.MUL to listOf(OperatorType.MUL),
        OperatorType.MUL_ASSIGN to listOf(OperatorType.MUL, OperatorType.ASSIGN),
    )

    private val superMap = mapOf(
        OperatorType.LESS to triBracketsMap,
        OperatorType.AMPERSAND to ampersandMap,
        OperatorType.MUL to multiplyMap,
    )

    private fun splitOperator(mapTag: OperatorType) {
        val map = superMap[mapTag] ?: return

        val t = peek()
        if (t !is Token.Operator) return
        val splitted = map[t.type] ?: return

        var offset = 0

        val newTokens = splitted.map {
            val newToken = t.copy(
                type = it,
                raw = langSpec.getOperatorInfo(it)?.symbol ?: "",
                pos = t.pos.copy(col = t.pos.col + offset),
            )
            offset += newToken.raw.length
            newToken
        }

        tokens.removeAt(index)
        tokens.addAll(index, newTokens)
    }

    override fun splitOperators(mapTag: OperatorType) {
        save()
        val map = superMap[mapTag] ?: return

        while (true) {
            val t = peek()
            if (t !is Token.Operator || t.type !in map) break
            splitOperator(mapTag = mapTag)
            next()
        }

        restore()
    }

    override fun getEnclosedTriBracketsEndToken(): Token {
        save()

        var depth = 1

        while (!match(Token.EOF::class) && depth > 0) {
            next()

            if (peek() !is Token.Operator) // pre
                continue

            splitOperator(mapTag = OperatorType.LESS)
            val t = peek()
            if (t !is Token.Operator) continue      // post

            depth += when (t.type) {
                OperatorType.LESS -> +1
                OperatorType.GREATER -> -1
                else -> continue
            }
        }

        val t = peek()
        restore()
        return t
    }

}