package lang.parser

import lang.core.KeywordType
import lang.core.SourceRange
import lang.core.operators.OperatorType
import lang.mappers.BinOpTypeMapper
import lang.mappers.UnaryOpTypeMapper
import lang.nodes.*
import lang.tokens.Token

object ParserUtils {
    fun ExprNode.wrapToBlock(check: Boolean = true): BlockNode {
        if (check && this is BlockNode) return this
        return BlockNode(nodes = listOf(this), range = range)
    }

    infix fun Token.isKeyword(type: KeywordType) = this is Token.Keyword && this.type == type

    infix fun Token.isOperator(type: OperatorType) = this is Token.Operator && this.type == type

    /*fun Token.isAccessOperator() = this is Token.Operator && (when (this.type) {
        OperatorType.DOT, OperatorType.SCOPE,  -> true
        else -> false
    })*/

    infix fun Token.isNotOperator(type: OperatorType) = this !is Token.Operator || this.type != type

    fun Token.Identifier.toIdentifierNode() = when (this) {
        is Token.This -> ThisIdentifierNode(range = range)
        is Token.Super -> SuperIdentifierNode(range = range)
        else -> IdentifierNode(value = raw, range = range)
    }

    fun ExprNode.flattenCommaNode(): List<ExprNode> {
        if (this !is BinOpNode || operator != BinOpType.COMMA) return listOf(this)
        val list = mutableListOf<ExprNode>()
        list.addAll(left.flattenCommaNode())
        list.addAll(right.flattenCommaNode())
        return list
    }

    fun IdentifierNode.toDatatype(): BaseDatatypeNode {
        return when (value) {
            AutoDatatypeNode.NAME -> AutoDatatypeNode(range = range)
            VoidDatatypeNode.NAME -> VoidDatatypeNode(range = range)
            else -> DatatypeNode(
                identifier = this, typeNames = null, isReference = false, isConst = false, range = range
            )
        }
    }

    fun ExprNode.tryConvertToDatatype(): BaseDatatypeNode? {
        return when (this) {
            is BaseDatatypeNode -> this
            is IdentifierNode -> this.toDatatype()
            else -> null
        }
    }

    val unaryOpTypeMapper = UnaryOpTypeMapper()
    val binOpTypeMapper = BinOpTypeMapper()


    fun OperatorType.isBinOperator() = binOpTypeMapper.toSecond(this) != null

    fun OperatorType.isUnaryOperator() = unaryOpTypeMapper.toSecond(this) != null

    fun <T : ExprNode> List<T>.range(defaultEmpty: SourceRange): SourceRange {
        return when (size) {
            0 -> defaultEmpty
            1 -> get(0).range
            else -> get(0).range untilEndOf get(1).range
        }
    }
}