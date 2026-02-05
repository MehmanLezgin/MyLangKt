package lang.core

import lang.tokens.KeywordInfo
import lang.tokens.OperatorInfo
import lang.tokens.OperatorType

interface ILangSpec {
    val keywords: List<KeywordInfo>
    val operators: Set<OperatorInfo>
    val moduleNameSeparator: OperatorType

    fun getKeywordInfo(value: String): KeywordInfo?
    fun getOperatorInfo(value: String): OperatorInfo?
    fun getOperatorInfo(type: OperatorType): OperatorInfo?
}