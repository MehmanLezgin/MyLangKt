package lang.tokens

import lang.messages.MsgHandler
import lang.core.LangSpec
import lang.core.ISourceCode
import lang.core.operators.OperatorMaps
import lang.core.operators.OperatorType
import lang.core.SourceRange
import lang.lexer.ILexer


class TokenStream(
    private val lexer: ILexer,
    private val src: ISourceCode,
    private val langSpec: LangSpec,
    msgHandler: MsgHandler
) : BaseTokenStream(
    lexer = lexer,
    msgHandler = msgHandler
) {
    private var tokens: MutableList<Token> = mutableListOf()
    private var index: Int = 0
    private val savedIndices: ArrayDeque<Int> = ArrayDeque()

    override val eof by lazy {
        tokens.findLast { it is Token.EOF } as? Token.EOF
            ?: Token.EOF(
                range = tokens.lastOrNull()?.range ?: SourceRange(src = src)
            )
    }

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
        else eof

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

    private fun splitOperator(mapTag: OperatorType) {
        val map = OperatorMaps.superMap[mapTag] ?: return

        val t = peek()
        if (t !is Token.Operator) return
        val splits = map[t.type] ?: return

        var offset = 0

        val newTokens = splits.map {
            val length = it.raw.length
            val newRange = t.range.horizontalCut(offset, length)

            val newToken = t.copy(
                type = it,
                raw = langSpec.getOperatorInfo(it)?.raw ?: "",
                range = newRange
            )

            offset += length
            newToken
        }

        tokens.removeAt(index)
        tokens.addAll(index, newTokens)
    }

    override fun splitOperators(mapTag: OperatorType) {
        save()
        0..1 step 1
        val map = OperatorMaps.superMap[mapTag] ?: return

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