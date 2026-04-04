package lang.infrastructure.operators


data class OperatorInfo(
    val type: OperatorType,
    val precedence: Int
) {
    val raw: String
        get() = type.raw
}