@file:OptIn(ExperimentalUnsignedTypes::class)

package lang.semantics.types

import lang.nodes.LiteralNode
import lang.semantics.builtin.PrimitivesScope.boolConst
import lang.semantics.builtin.PrimitivesScope.charConst
import lang.semantics.builtin.PrimitivesScope.constCharPtr
import lang.semantics.builtin.PrimitivesScope.float32Const
import lang.semantics.builtin.PrimitivesScope.float64Const
import lang.semantics.builtin.PrimitivesScope.int16Const
import lang.semantics.builtin.PrimitivesScope.int32Const
import lang.semantics.builtin.PrimitivesScope.int64Const
import lang.semantics.builtin.PrimitivesScope.int8Const
import lang.semantics.builtin.PrimitivesScope.uint16Const
import lang.semantics.builtin.PrimitivesScope.uint32Const
import lang.semantics.builtin.PrimitivesScope.uint64Const
import lang.semantics.builtin.PrimitivesScope.uint8Const
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.builtin.types.BoolPrimitive
import lang.semantics.builtin.types.CharPrimitive
import lang.semantics.builtin.types.Float32Primitive
import lang.semantics.builtin.types.Float64Primitive
import lang.semantics.builtin.types.Int16Primitive
import lang.semantics.builtin.types.Int32Primitive
import lang.semantics.builtin.types.Int64Primitive
import lang.semantics.builtin.types.Int8Primitive
import lang.semantics.builtin.types.UCharPrimitive
import lang.semantics.builtin.types.UInt16Primitive
import lang.semantics.builtin.types.UInt32Primitive
import lang.semantics.builtin.types.UInt64Primitive
import lang.semantics.builtin.types.UInt8Primitive


@OptIn(ExperimentalStdlibApi::class)
data class ConstValue<T : Any>(
    val value: T,
    val type: Type = fromValue(value) ?: throw Exception("Invalid constant value")
) {
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
        val t1 = if (this.type == PrimitivesScope.bool) PrimitivesScope.int32 else this.type
        val t2 = if (other.type == PrimitivesScope.bool) PrimitivesScope.int32 else other.type
        return highest(t1, t2)
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
    infix fun shl(other: ConstValue<*>) = ConstValue(this.toNumber().toLong() shl other.toNumber().toInt(), type)
    infix fun shr(other: ConstValue<*>) = ConstValue(this.toNumber().toLong() shr other.toNumber().toInt(), type)
    infix fun and(other: ConstValue<*>) = ConstValue(this.toNumber().toLong() and other.toNumber().toLong(), type)
    infix fun or(other: ConstValue<*>) = ConstValue(this.toNumber().toLong() or other.toNumber().toLong(), type)
    infix fun xor(other: ConstValue<*>) = ConstValue(this.toNumber().toLong() xor other.toNumber().toLong(), type)

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
        fun highest(a: PrimitiveType, b: PrimitiveType) = if (a.prec >= b.prec) a else b

        @OptIn(ExperimentalUnsignedTypes::class)
        fun fromValue(value: Any): Type? = when (value) {
            is Boolean -> boolConst
            is Byte -> int8Const
            is UByte -> uint8Const
            is Short -> int16Const
            is UShort -> uint16Const
            is Int -> int32Const
            is UInt -> uint32Const
            is Long -> int64Const
            is ULong -> uint64Const
            is Float -> float32Const
            is Double -> float64Const
            is Char -> charConst
            is String -> constCharPtr
            else -> null
        }?.setFlags(
            isConst = true,
            isExprType = true
        )


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
                is BoolPrimitive -> n.toDouble() != 0.0
                is Int8Primitive -> n.toByte()
                is UInt8Primitive -> (n.toInt() and 0xFF).toUByte()
                is Int16Primitive -> n.toShort()
                is UInt16Primitive -> (n.toInt() and 0xFFFF).toUShort()
                is Int32Primitive -> n.toInt()
                is UInt32Primitive -> (n.toLong() and 0xFFFFFFFF).toUInt()
                is Int64Primitive -> n.toLong()
                is UInt64Primitive -> (n.toLong()).toULong()
                is Float32Primitive -> when (n) {
                    is Float -> n
                    else -> n.toFloat()
                }

                is Float64Primitive -> when (n) {
                    is Double -> n
                    else -> n.toDouble()
                }

                is CharPrimitive -> n.toInt().toChar()
                is UCharPrimitive -> (n.toInt() and 0xFF).toUByte()
                else -> null
            }
        }
    }
}
