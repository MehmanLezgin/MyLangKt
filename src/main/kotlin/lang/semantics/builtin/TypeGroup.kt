package lang.semantics.builtin

import lang.semantics.types.Type
import lang.tokens.OperatorType

class TypeGroup(
    private val types: List<Type>
) {

    fun arithmetic() =
        ops(
            OperatorType.PLUS,
            OperatorType.MINUS,
            OperatorType.MUL,
            OperatorType.DIV,
            OperatorType.REMAINDER,
            OperatorType.PLUS_ASSIGN,
            OperatorType.MINUS_ASSIGN,
            OperatorType.MUL_ASSIGN,
            OperatorType.DIV_ASSIGN,
            OperatorType.REMAINDER_ASSIGN
        )

    fun shift() =
        ops(
            OperatorType.SHIFT_LEFT, OperatorType.SHIFT_RIGHT,
            OperatorType.SHIFT_LEFT_ASSIGN, OperatorType.SHIFT_RIGHT_ASSIGN
        )

    fun bitwise() =
        ops(
            OperatorType.AMPERSAND, OperatorType.BIN_OR, OperatorType.BIN_XOR,
            OperatorType.BIN_AND_ASSIGN, OperatorType.BIN_OR_ASSIGN, OperatorType.BIN_XOR_ASSIGN
        )

    fun logical() =
        ops(OperatorType.AND, OperatorType.OR)

    fun eq() =
        ops(OperatorType.EQUAL, OperatorType.NOT_EQUAL)

    fun compare() =
        ops(
            OperatorType.LESS,
            OperatorType.LESS_EQUAL,
            OperatorType.GREATER,
            OperatorType.GREATER_EQUAL,
            OperatorType.EQUAL,
            OperatorType.NOT_EQUAL
        )

    fun ops(vararg operators: OperatorType) {
        types.forEach { t ->
            operators.forEach { op ->
                createOperFunc(op, returnType = t, t, t)
            }
        }
    }


}
