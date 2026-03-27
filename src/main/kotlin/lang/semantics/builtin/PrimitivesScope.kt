package lang.semantics.builtin

import lang.messages.MsgHandler
import lang.semantics.builtin.modules.mathModule
import lang.semantics.builtin.modules.stdModule
import lang.semantics.builtin.types.*
import lang.semantics.scopes.Scope
import lang.semantics.types.*

object PrimitivesScope : Scope(
    parent = null,
) {

    // ========= TYPES =========

    val bool = BoolPrimitive()
    val boolConst by lazyConst(bool)

    // ---- CHAR ----
    val char = CharPrimitive()
    val uchar = UCharPrimitive()

    val charConst by lazyConst(char)
    val ucharConst by lazyConst(uchar)

    // ---- 8 BIT ----
    val int8 = Int8Primitive()
    val uint8 = UInt8Primitive()

    val int8Const by lazyConst(int8)
    val uint8Const by lazyConst(uint8)

    // ---- 16 BIT ----
    val int16 = Int16Primitive()
    val uint16 = UInt16Primitive()

    val int16Const by lazyConst(int16)
    val uint16Const by lazyConst(uint16)

    // ---- 32 BIT ----
    val int32 = Int32Primitive()
    val uint32 = UInt32Primitive()

    val int32Const by lazyConst(int32)
    val uint32Const by lazyConst(uint32)

    // ---- 64 BIT ----
    val int64 = Int64Primitive()
    val uint64 = UInt64Primitive()

    val int64Const by lazyConst(int64)
    val uint64Const by lazyConst(uint64)

    // ---- FLOAT ----
    val float32 = Float32Primitive()
    val float64 = Float64Primitive()

    val float32Const by lazyConst(float32)
    val float64Const by lazyConst(float64)

    val void = VoidPrimitive()

    val voidPtr by lazy { PointerType(base = void, level = 1) }

    val constCharPtr by lazy {
        PointerType(
            base = char,
            level = 1,
            flags = TypeFlags(isConst = true)
        )
    }

    private val signedInts = listOf(int32, int8, int16, int64)
    private val unsignedInts = listOf(uint8, uint16, uint32, uint64)
    private val ints = signedInts + unsignedInts
    private val floats = listOf(float32, float64)

    internal val allPrimitives: List<PrimitiveType> =
        ints + floats + void + char + uchar + bool


    val ptrOperPlus = ptrOperPlus()
    val ptrOperMinus = ptrOperMinus()
    val ptrOperEq = ptrOperEq()
    val ptrOperNotEq = ptrOperNotEq()
    val ptrOperGrThan = ptrOperGrThan()
    val ptrOperLessThan = ptrOperLessThan()
    val ptrOperGrEqThan = ptrOperGrEqThan()
    val ptrOperLessEqThan = ptrOperLessEqThan()

    val builtInModules = listOf(
        mathModule(),
        stdModule()
    )

    fun lazyConst(type: PrimitiveType) = lazy { type.toConst() }


    init {
        try {
            val msgHandler = MsgHandler()

            primitives(allPrimitives)

            if (msgHandler.hasErrors)
                println(msgHandler.formatErrors())

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
