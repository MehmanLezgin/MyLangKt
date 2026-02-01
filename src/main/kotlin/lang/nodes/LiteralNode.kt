@file:OptIn(ExperimentalUnsignedTypes::class)

package lang.nodes

import lang.tokens.Pos
import kotlin.Boolean
import kotlin.Char
import kotlin.Float
import kotlin.Int
import kotlin.UInt

sealed class LiteralNode<T: Any>(
    open val value: T,
    override val pos: Pos
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode =
        mapper(this)

    data class IntLiteral(override val value: Int, override val pos: Pos) : LiteralNode<Int>(value, pos)
    data class LongLiteral(override val value: Long, override val pos: Pos) : LiteralNode<Long>(value, pos)
    data class UIntLiteral(override val value: UInt, override val pos: Pos) : LiteralNode<UInt>(value, pos)
    data class ULongLiteral(override val value: ULong, override val pos: Pos) : LiteralNode<ULong>(value, pos)
    data class FloatLiteral(override val value: Float, override val pos: Pos) : LiteralNode<Float>(value, pos)
    data class DoubleLiteral(override val value: Double, override val pos: Pos) : LiteralNode<Double>(value, pos)
    data class BooleanLiteral(override val value: Boolean, override val pos: Pos) : LiteralNode<Boolean>(value, pos)
    data class StringLiteral(override val value: String, override val pos: Pos) : LiteralNode<String>(value, pos)
    data class CharLiteral(override val value: Char, override val pos: Pos) : LiteralNode<Char>(value, pos)
}

data class NullLiteralNode(
    override val pos: Pos
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode =
        mapper(this)
}