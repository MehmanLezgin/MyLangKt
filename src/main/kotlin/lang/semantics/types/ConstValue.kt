@file:OptIn(ExperimentalUnsignedTypes::class)

package lang.semantics.types


@OptIn(ExperimentalStdlibApi::class)
data class ConstValue<T : Any>(val value: T) {

    val type: Type = BuiltInTypes.fromValue(value) ?: throw Exception("Invalid constant value")

    private fun toNumber(): Number = when (value) {
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
        val t1 = if (this.type == BuiltInTypes.bool) BuiltInTypes.int32 else this.type
        val t2 = if (other.type == BuiltInTypes.bool) BuiltInTypes.int32 else other.type
        return BuiltInTypes.highest(t1, t2)
    }

    private fun convertToType(targetType: Type, n: Number): Any? = when (targetType) {
        BuiltInTypes.boolConst -> n.toDouble() != 0.0
        BuiltInTypes.int8Const -> n.toByte()
        BuiltInTypes.uint8Const -> (n.toInt() and 0xFF).toUByte()
        BuiltInTypes.int16Const -> n.toShort()
        BuiltInTypes.uint16Const -> (n.toInt() and 0xFFFF).toUShort()
        BuiltInTypes.int32Const -> n.toInt()
        BuiltInTypes.uint32Const -> (n.toLong() and 0xFFFFFFFF).toUInt()
        BuiltInTypes.int64Const -> n.toLong()
        BuiltInTypes.uint64Const -> (n.toLong()).toULong()
        BuiltInTypes.float32Const -> when (n) {
            is Float -> n
            else -> n.toFloat()
        }

        BuiltInTypes.float64Const -> when (n) {
            is Double -> n
            else -> n.toDouble()
        }

        BuiltInTypes.charConst -> n.toInt().toChar()
        BuiltInTypes.ucharConst -> (n.toInt() and 0xFF).toUByte()
        else -> null
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
}
