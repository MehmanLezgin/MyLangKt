@file:OptIn(ExperimentalUnsignedTypes::class)

package lang.semantics.types


enum class ConstType(val precedence: Int) {
    BOOLEAN(0),
    UCHAR(1),
    CHAR(1),
    INT8(2),
    UINT8(2),
    INT16(3),
    UINT16(3),
    INT(4),
    UINT(4),
    LONG(5),
    ULONG(5),
    FLOAT(6),
    DOUBLE(7),
    STRING(8);

    companion object {
        fun fromValue(value: Any): ConstType = when (value) {
            is Boolean -> BOOLEAN
            is Byte -> INT8
            is UByte -> UINT8
            is Short -> INT16
            is UShort -> UINT16
            is Int -> INT
            is UInt -> UINT
            is Long -> LONG
            is ULong -> ULONG
            is Float -> FLOAT
            is Double -> DOUBLE
            is Char -> CHAR
            else -> STRING
        }

        fun highest(a: ConstType, b: ConstType) = if (a.precedence >= b.precedence) a else b
    }
}


@OptIn(ExperimentalStdlibApi::class)
data class ConstValue<T : Any>(val value: T) {

    private val type: ConstType = ConstType.fromValue(value)

    private fun toNumber(): Number = when (value) {
        is Boolean -> if (value) 1 else 0
        is Byte, is Short, is Int, is Long,
        is UByte, is UShort, is UInt, is ULong,
        is Float, is Double -> value as Number
        is Char -> value.code
        else -> 0 // NEVER ERROR
    }

    private fun toBoolean(): Boolean = when (value) {
        is Boolean -> value
        is Number -> value.toDouble() != 0.0
        is Char -> value.code != 0
        else -> false
    }

    private fun promoteType(other: ConstValue<*>): ConstType {
        val t1 = if (this.type == ConstType.BOOLEAN) ConstType.INT else this.type
        val t2 = if (other.type == ConstType.BOOLEAN) ConstType.INT else other.type
        return ConstType.highest(t1, t2)
    }

    private fun convertToType(targetType: ConstType, n: Number): Any? = when (targetType) {
        ConstType.BOOLEAN -> n.toDouble() != 0.0
        ConstType.INT8 -> n.toByte()
        ConstType.UINT8 -> (n.toInt() and 0xFF).toUByte()
        ConstType.INT16 -> n.toShort()
        ConstType.UINT16 -> (n.toInt() and 0xFFFF).toUShort()
        ConstType.INT -> n.toInt()
        ConstType.UINT -> (n.toLong() and 0xFFFFFFFF).toUInt()
        ConstType.LONG -> n.toLong()
        ConstType.ULONG -> (n.toLong()).toULong()
        ConstType.FLOAT -> when(n) { is Float -> n else -> n.toFloat() }
        ConstType.DOUBLE -> when(n) { is Double -> n else -> n.toDouble() }
        ConstType.CHAR -> n.toInt().toChar()
        ConstType.UCHAR -> (n.toInt() and 0xFF).toUByte()
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
        convertToType(type, toNumber())!!.let { ConstValue(it) }

    // Unary -
    fun unaryMinus(): ConstValue<*> =
        convertToType(type, -toNumber().toDouble())!!.let { ConstValue(it) }

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
        val targetType = promoteType(other)
        val result = op(this.toNumber(), other.toNumber())
        return convertToType(targetType, result)?.let { ConstValue(it) }
    }
}
