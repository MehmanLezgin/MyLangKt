@file:OptIn(ExperimentalUnsignedTypes::class)

package lang.nodes

import lang.core.SourceRange

sealed class LiteralNode<T: Any>(
    open val value: T,
    override val range: SourceRange
) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode =
        mapper(this)

    data class IntLiteral(override val value: Int, override val range: SourceRange) : LiteralNode<Int>(value, range)
    data class LongLiteral(override val value: Long, override val range: SourceRange) : LiteralNode<Long>(value, range)
    data class UIntLiteral(override val value: UInt, override val range: SourceRange) : LiteralNode<UInt>(value, range)
    data class ULongLiteral(override val value: ULong, override val range: SourceRange) : LiteralNode<ULong>(value, range)
    data class FloatLiteral(override val value: Float, override val range: SourceRange) : LiteralNode<Float>(value, range)
    data class DoubleLiteral(override val value: Double, override val range: SourceRange) : LiteralNode<Double>(value, range)
    data class BooleanLiteral(override val value: Boolean, override val range: SourceRange) : LiteralNode<Boolean>(value, range)
    data class StringLiteral(override val value: String, override val range: SourceRange) : LiteralNode<String>(value, range)
    data class CharLiteral(override val value: Char, override val range: SourceRange) : LiteralNode<Char>(value, range)
}

data class NullLiteralNode(
    override val range: SourceRange
) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode =
        mapper(this)
}