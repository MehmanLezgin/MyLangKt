package lang.core

import lang.core.operators.OperatorInfo
import lang.core.operators.OperatorType
import lang.core.operators.operatorPrecedence

object LangSpec {
    val moduleNameSeparator: OperatorType = OperatorType.SCOPE

    val keywords = KeywordType.values().associate { type ->
        type.value to KeywordInfo(type = type)
    }

    val operators: Map<String, OperatorInfo>

    val InfixOperator: OperatorInfo

    init {
        // initializing operators
        val map = operatorPrecedence()
        val key = OperatorType.INFIX.raw
        InfixOperator = map[key]!! // saving an infix oper info with precedence
        map.remove(key)
        operators = map
    }

    fun getKeywordInfo(value: String): KeywordInfo? {
        return keywords[value]
    }

    fun getOperatorInfo(value: String): OperatorInfo? {
        return operators[value]
    }

    fun getOperatorInfo(type: OperatorType): OperatorInfo? {
        return operators[type.raw]
    }
}