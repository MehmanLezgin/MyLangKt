package lang.parser

import lang.tokens.KeywordType
import lang.tokens.OperatorType
import lang.nodes.AutoDatatypeNode
import lang.nodes.BaseDatatypeNode
import lang.nodes.BinOpNode
import lang.nodes.BinOpType
import lang.nodes.BlockNode
import lang.nodes.DatatypeNode
import lang.nodes.ExprNode
import lang.nodes.IdentifierNode
import lang.nodes.VoidDatatypeNode
import lang.tokens.Token
import kotlin.reflect.KClass

object ParserUtils {
    fun ExprNode.wrapToBody(check: Boolean = true): BlockNode {
        if (check && this is BlockNode) return this
        return BlockNode(nodes = listOf(this), pos = pos)
    }

    infix fun Token.isKeyword(type: KeywordType) =
        this is Token.Keyword && this.type == type

    fun Token.isKeyword(vararg types: KeywordType) =
        this is Token.Keyword && types.any { it == this.type }

    infix fun Token.isOperator(type: OperatorType) =
        this is Token.Operator && this.type == type

    infix fun Token.isNotOperator(type: OperatorType) =
        this !is Token.Operator || this.type != type

    fun Token.isOperator(vararg types: KClass<out OperatorType>) =
        this is Token.Operator && types.any { it.isInstance(this.type) }

    fun Token.Keyword.isModifier() = this.isKeyword(
        KeywordType.CONST,
        KeywordType.PRIVATE,
        KeywordType.PUBLIC,
        KeywordType.PROTECTED,
        KeywordType.STATIC,
        KeywordType.OPEN,
        KeywordType.OVERRIDE
    )

    fun Token.Identifier.toIdentifierNode() =
        IdentifierNode(
            value = value,
            pos = pos
        )

    fun ExprNode.flattenCommaNode(): List<ExprNode> {
        if (this !is BinOpNode || operator != BinOpType.COMMA) return listOf(this)
        val list = mutableListOf<ExprNode>()
        list.addAll(left.flattenCommaNode())
        list.addAll(right.flattenCommaNode())
        return list
    }

    fun IdentifierNode.toDatatype(): BaseDatatypeNode {
        return when (value) {
            AutoDatatypeNode.NAME -> AutoDatatypeNode(pos = pos)
            VoidDatatypeNode.NAME -> VoidDatatypeNode(pos = pos)
            else -> DatatypeNode(
                identifier = this,
                typeNames = null,
                isReference = false,
                isConst = false,
                pos = pos
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

    infix fun ExprNode.isBinOperator(type: BinOpType) =
        this is BinOpNode && this.operator == type

    infix fun ExprNode.isNotBinOperator(type: BinOpType) =
        this !is BinOpNode || this.operator != type

    val simpleUnaryOps = listOf(
        OperatorType.PLUS, OperatorType.MINUS,
        OperatorType.NOT, OperatorType.BIN_NOT,
        OperatorType.SIZEOF, OperatorType.NEW,
        OperatorType.DELETE, OperatorType.NOT_NULL_ASSERTION,
        OperatorType.AMPERSAND, OperatorType.MUL,
        OperatorType.IS
    )

    fun OperatorType.isSimpleUnaryOp() = this in simpleUnaryOps
}