@file:OptIn(ExperimentalUnsignedTypes::class)

package lang.semantics.types

import lang.nodes.LiteralNode


@OptIn(ExperimentalStdlibApi::class)
data class ConstValue<T : Any>(val value: T) {

    val type: Type = PrimitiveTypes.fromValue(value) ?: throw Exception("Invalid constant value")

    fun toNumber(): Number = when (value) {
        is Boolean -> if (value) 1 else 0

        is Byte, is Short,
        is Int, is Long,
        is Float, is Double -> value as Number

        is UByte -> value.toInt()
        is UShort -> value.toInt()
        is UInt -> value.toLong()
        is ULong -> value.toLong()

        is Char -> value.code
        else -> 0
    }

    private fun toBoolean(): Boolean = when (value) {
        is Boolean -> value
        is Number -> value.toDouble() != 0.0
        is Char -> value.code != 0
        else -> false
    }

    private fun promoteType(other: ConstValue<*>): PrimitiveType? {
        if (this.type !is PrimitiveType || other.type !is PrimitiveType) return null
        val t1 = if (this.type == PrimitiveTypes.bool) PrimitiveTypes.int32 else this.type
        val t2 = if (other.type == PrimitiveTypes.bool) PrimitiveTypes.int32 else other.type
        return PrimitiveTypes.highest(t1, t2)
    }

    // Arithmetic
    operator fun plus(other: ConstValue<*>) = operate(other) { a, b -> a.toDouble() + b.toDouble() }
    operator fun minus(other: ConstValue<*>) = operate(other) { a, b -> a.toDouble() - b.toDouble() }
    operator fun times(other: ConstValue<*>) = operate(other) { a, b -> a.toDouble() * b.toDouble() }
    operator fun div(other: ConstValue<*>) = operate(other) { a, b -> a.toDouble() / b.toDouble() }
    operator fun rem(other: ConstValue<*>) = operate(other) { a, b -> a.toDouble() % b.toDouble() }

    // Unary +
    fun unaryPlus(): ConstValue<*> =
        ConstValue(convertToType(type, toNumber())!!)

    // Unary -
    fun unaryMinus(): ConstValue<*> =
        ConstValue(convertToType(type, -toNumber().toDouble())!!)

    // Bitwise NOT (~)
    fun bitwiseNot(): ConstValue<*> =
        ConstValue(toNumber().toLong().inv())

    fun logicalNot(): ConstValue<Boolean> =
        ConstValue(!toBoolean())

    // Bitwise (always convert to Long/ULong)
    infix fun shl(other: ConstValue<*>) = ConstValue((this.toNumber().toLong() shl other.toNumber().toInt()))
    infix fun shr(other: ConstValue<*>) = ConstValue((this.toNumber().toLong() shr other.toNumber().toInt()))
    infix fun and(other: ConstValue<*>) = ConstValue(this.toNumber().toLong() and other.toNumber().toLong())
    infix fun or(other: ConstValue<*>) = ConstValue(this.toNumber().toLong() or other.toNumber().toLong())
    infix fun xor(other: ConstValue<*>) = ConstValue(this.toNumber().toLong() xor other.toNumber().toLong())

    // Comparisons (result is Boolean)
    infix fun less(other: ConstValue<*>) = ConstValue(this.toNumber().toDouble() < other.toNumber().toDouble())
    infix fun lessEqual(other: ConstValue<*>) = ConstValue(this.toNumber().toDouble() <= other.toNumber().toDouble())
    infix fun greater(other: ConstValue<*>) = ConstValue(this.toNumber().toDouble() > other.toNumber().toDouble())
    infix fun greaterEqual(other: ConstValue<*>) = ConstValue(this.toNumber().toDouble() >= other.toNumber().toDouble())
    infix fun equal(other: ConstValue<*>) = ConstValue(this.toNumber().toDouble() == other.toNumber().toDouble())
    infix fun notEqual(other: ConstValue<*>) = ConstValue(this.toNumber().toDouble() != other.toNumber().toDouble())

    // Logical (convert to bool first)
    infix fun logicalAnd(other: ConstValue<*>) = ConstValue(this.toBoolean() && other.toBoolean())
    infix fun logicalOr(other: ConstValue<*>) = ConstValue(this.toBoolean() || other.toBoolean())

    private fun operate(other: ConstValue<*>, op: (Number, Number) -> Number): ConstValue<*>? {
        val targetType = promoteType(other) ?: return null
        val result = op(this.toNumber(), other.toNumber())
        return convertToType(targetType, result)?.let { ConstValue(it) }
    }

    companion object {
        fun from(literal: LiteralNode<*>): ConstValue<*>? {
            return when (literal) {
                is LiteralNode.BooleanLiteral -> ConstValue(literal.value)
                is LiteralNode.CharLiteral -> ConstValue(literal.value)
                is LiteralNode.DoubleLiteral -> ConstValue(literal.value)
                is LiteralNode.FloatLiteral -> ConstValue(literal.value)
                is LiteralNode.IntLiteral -> ConstValue(literal.value)
                is LiteralNode.LongLiteral -> ConstValue(literal.value)
                is LiteralNode.StringLiteral -> ConstValue(literal.value)
                is LiteralNode.UIntLiteral -> ConstValue(literal.value)
                is LiteralNode.ULongLiteral -> ConstValue(literal.value)
            }
        }

        fun convertToType(targetType: Type, n: Number): Any? {
            return when (targetType) {
                PrimitiveTypes.boolConst -> n.toDouble() != 0.0
                PrimitiveTypes.int8Const -> n.toByte()
                PrimitiveTypes.uint8Const -> (n.toInt() and 0xFF).toUByte()
                PrimitiveTypes.int16Const -> n.toShort()
                PrimitiveTypes.uint16Const -> (n.toInt() and 0xFFFF).toUShort()
                PrimitiveTypes.int32Const -> n.toInt()
                PrimitiveTypes.uint32Const -> (n.toLong() and 0xFFFFFFFF).toUInt()
                PrimitiveTypes.int64Const -> n.toLong()
                PrimitiveTypes.uint64Const -> (n.toLong()).toULong()
                PrimitiveTypes.float32Const -> when (n) {
                    is Float -> n
                    else -> n.toFloat()
                }

                PrimitiveTypes.float64Const -> when (n) {
                    is Double -> n
                    else -> n.toDouble()
                }

                PrimitiveTypes.charConst -> n.toInt().toChar()
                PrimitiveTypes.ucharConst -> (n.toInt() and 0xFF).toUByte()
                else -> null
            }
        }
    }
}
