package lang.core.operators

object OperatorMaps {
    val triBracketsMap = mapOf(
        OperatorType.LESS_EQUAL to listOf(OperatorType.LESS, OperatorType.ASSIGN),
        OperatorType.GREATER_EQUAL to listOf(OperatorType.GREATER, OperatorType.ASSIGN),

        OperatorType.SHIFT_LEFT to listOf(OperatorType.LESS, OperatorType.LESS),
        OperatorType.SHIFT_LEFT_ASSIGN to listOf(OperatorType.LESS, OperatorType.LESS, OperatorType.ASSIGN),

        OperatorType.SHIFT_RIGHT to listOf(OperatorType.GREATER, OperatorType.GREATER),
        OperatorType.SHIFT_RIGHT_ASSIGN to listOf(OperatorType.GREATER, OperatorType.GREATER, OperatorType.ASSIGN),
    )

    val ampersandMap = mapOf(
        OperatorType.AMPERSAND to listOf(OperatorType.AMPERSAND),
        OperatorType.AND to listOf(OperatorType.AMPERSAND, OperatorType.AMPERSAND),
        OperatorType.BIN_AND_ASSIGN to listOf(OperatorType.AMPERSAND, OperatorType.ASSIGN)
    )

    val multiplyMap = mapOf(
        OperatorType.MUL to listOf(OperatorType.MUL),
        OperatorType.MUL_ASSIGN to listOf(OperatorType.MUL, OperatorType.ASSIGN),
    )

    val superMap = mapOf(
        OperatorType.LESS to triBracketsMap,
        OperatorType.AMPERSAND to ampersandMap,
        OperatorType.MUL to multiplyMap,
    )
}