package lang.mappers

import lang.tokens.OperatorType
import lang.nodes.BinOpType

class BinOpTypeMapper : IOneWayMapper<OperatorType, BinOpType?> {
    private val map = mapOf(
        OperatorType.ARROW to BinOpType.ARROW,
        OperatorType.COMMA to BinOpType.COMMA,
        OperatorType.COLON to BinOpType.COLON,
        OperatorType.MUL to BinOpType.MUL,
        OperatorType.DIV to BinOpType.DIV,
        OperatorType.REMAINDER to BinOpType.REMAINDER,
        OperatorType.PLUS to BinOpType.PLUS,
        OperatorType.MINUS to BinOpType.MINUS,
        OperatorType.SHIFT_LEFT to BinOpType.SHIFT_LEFT,
        OperatorType.SHIFT_RIGHT to BinOpType.SHIFT_RIGHT,
        OperatorType.LESS to BinOpType.LESS,
        OperatorType.LESS_EQUAL to BinOpType.LESS_EQUAL,
        OperatorType.GREATER to BinOpType.GREATER,
        OperatorType.GREATER_EQUAL to BinOpType.GREATER_EQUAL,
        OperatorType.EQUAL to BinOpType.EQUAL,
        OperatorType.NOT_EQUAL to BinOpType.NOT_EQUAL,
        OperatorType.AMPERSAND to BinOpType.BIN_AND,
        OperatorType.BIN_XOR to BinOpType.BIN_XOR,
        OperatorType.BIN_OR to BinOpType.BIN_OR,
        OperatorType.AND to BinOpType.AND,
        OperatorType.OR to BinOpType.OR,
        OperatorType.ASSIGN to BinOpType.ASSIGN,
//        OperatorType.PLUS_ASSIGN to BinOpType.PLUS_ASSIGN,
//        OperatorType.MINUS_ASSIGN to BinOpType.MINUS_ASSIGN,
//        OperatorType.MUL_ASSIGN to BinOpType.MUL_ASSIGN,
//        OperatorType.DIV_ASSIGN to BinOpType.DIV_ASSIGN,
//        OperatorType.REMAINDER_ASSIGN to BinOpType.REMAINDER_ASSIGN,
//        OperatorType.BIN_AND_ASSIGN to BinOpType.AND_ASSIGN,
//        OperatorType.BIN_OR_ASSIGN to BinOpType.OR_ASSIGN,
//        OperatorType.BIN_XOR_ASSIGN to BinOpType.XOR_ASSIGN,
//        OperatorType.SHIFT_LEFT_ASSIGN to BinOpType.SHIFT_LEFT_ASSIGN,
//        OperatorType.SHIFT_RIGHT_ASSIGN to BinOpType.SHIFT_RIGHT_ASSIGN,
        OperatorType.AS to BinOpType.CAST,
        OperatorType.IS to BinOpType.IS,
        OperatorType.IN to BinOpType.IN,
        OperatorType.UNTIL to BinOpType.UNTIL,
        OperatorType.ELVIS to BinOpType.ELVIS,
        OperatorType.DOUBLE_DOT to BinOpType.DOUBLE_DOT,
    )

    override fun toSecond(a: OperatorType) = map[a]
}