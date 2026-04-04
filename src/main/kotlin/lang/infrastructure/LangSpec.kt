package lang.infrastructure

import lang.infrastructure.operators.OperatorInfo
import lang.infrastructure.operators.OperatorType
import lang.infrastructure.operators.operatorPrecedence
import lang.messages.Terms

object LangSpec {
    val moduleNameSeparator: OperatorType = OperatorType.SCOPE

    val keywords = KeywordType.entries.associate { type ->
        type.value to KeywordInfo(type = type)
    }

    val reservedIdentifierNames = setOf(
        Terms.THIS,
        Terms.SUPER
    )

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

    fun isReservedName(name: String) =
        reservedIdentifierNames.contains(name)

}