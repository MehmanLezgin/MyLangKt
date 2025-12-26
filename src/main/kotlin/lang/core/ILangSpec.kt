package lang.core

import lang.tokens.KeywordInfo
import lang.tokens.OperatorInfo
import lang.tokens.OperatorType
import lang.tokens.TokenRule

interface ILangSpec {
    val keywords: List<KeywordInfo>
    val operators: Set<OperatorInfo>
    val tokenRules: List<TokenRule>

    fun getKeywordInfo(value: String): KeywordInfo?
    fun getOperatorInfo(value: String): OperatorInfo?
    fun getOperatorInfo(type: OperatorType): OperatorInfo?
}