package lang.mappers

import lang.tokens.OperatorType
import lang.nodes.UnaryOpType

class UnaryOpTypeMapper : IOneWayMapper<OperatorType, UnaryOpType?> {
    private val map = mapOf(
        OperatorType.INCREMENT to UnaryOpType.INCREMENT,
        OperatorType.DECREMENT to UnaryOpType.DECREMENT,
        OperatorType.PLUS to UnaryOpType.PLUS,
        OperatorType.MINUS to UnaryOpType.MINUS,
        OperatorType.NOT to UnaryOpType.NOT,
        OperatorType.BIN_NOT to UnaryOpType.BITWISE_NOT,
        OperatorType.SIZEOF to UnaryOpType.SIZEOF,
        OperatorType.NEW to UnaryOpType.NEW,
        OperatorType.DELETE to UnaryOpType.DELETE,
        OperatorType.IS to UnaryOpType.IS,
        OperatorType.AMPERSAND to UnaryOpType.ADDRESS_OF,
        OperatorType.MUL to UnaryOpType.INDIRECTION,

        OperatorType.NOT_NULL_ASSERTION to UnaryOpType.NOT_NULL_ASSERTION,
    )

    override fun toSecond(a: OperatorType) = map[a]
}