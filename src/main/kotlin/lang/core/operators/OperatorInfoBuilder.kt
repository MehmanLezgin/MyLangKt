package lang.core.operators

typealias OperatorInfoMap = MutableMap<String, OperatorInfo>

class OperatorInfoBuilder {
    private var currentPrecedence: Int = OperatorType.values().size - 1
    private val operators: OperatorInfoMap = mutableMapOf()

    fun addOper(operType: OperatorType): OperatorInfo {
        val info = OperatorInfo(operType, currentPrecedence)
        operators[operType.raw] = info
        return info
    }

    fun nextLevel() {
        currentPrecedence--
    }

    fun build() = operators
}

fun OperatorInfoBuilder.level(block: OperatorInfoBuilder.() -> Unit) {
    block()
    nextLevel()
}

fun OperatorInfoBuilder.oper(oper: OperatorType) : OperatorInfo {
    return addOper(oper)
}

fun operatorsInfo(block: OperatorInfoBuilder.() -> Unit) : OperatorInfoMap {
    return OperatorInfoBuilder().apply(block).build()
}