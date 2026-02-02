package lang.semantics.builtin

import lang.messages.ErrorHandler
import lang.semantics.builtin.modules.mathModule
import lang.semantics.builtin.types.*
import lang.semantics.scopes.Scope
import lang.semantics.types.*

object PrimitivesScope : Scope(
    parent = null,
    errorHandler = ErrorHandler()
) {

    // ========= TYPES =========

    val bool = BoolPrimitive()
    val boolConst by lazy { bool.toConst() }

    // ---- CHAR ----
    val char = CharPrimitive()
    val uchar = UCharPrimitive()

    val charConst by lazy { char.toConst() }
    val ucharConst by lazy { uchar.toConst() }

    // ---- 8 BIT ----
    val int8 = Int8Primitive()
    val uint8 = UInt8Primitive()

    val int8Const by lazy { int8.toConst() }
    val uint8Const by lazy { uint8.toConst() }

    // ---- 16 BIT ----
    val int16 = Int16Primitive()
    val uint16 = UInt16Primitive()

    val int16Const by lazy { int16.toConst() }
    val uint16Const by lazy { uint16.toConst() }

    // ---- 32 BIT ----
    val int32 = Int32Primitive()
    val uint32 = UInt32Primitive()

    val int32Const by lazy {
        int32.toConst()
    }
    val uint32Const by lazy { uint32.toConst() }

    // ---- 64 BIT ----
    val int64 = Int64Primitive()
    val uint64 = UInt64Primitive()

    val int64Const by lazy { int64.toConst() }
    val uint64Const by lazy { uint64.toConst() }

    // ---- FLOAT ----
    val float32 = Float32Primitive()
    val float64 = Float64Primitive()

    val float32Const by lazy { float32.toConst() }
    val float64Const by lazy { float64.toConst() }

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

    val ptrOperPlus = buildPtrOperPlus()
    val ptrOperMinus = buildPtrOperMinus()
    val ptrOperEq = buildPtrOperEq()
    val ptrOperNotEq = buildPtrOperNotEq()
    val ptrOperGrThan = buildPtrOperGrThan()
    val ptrOperLessThan = buildPtrOperLessThan()
    val ptrOperGrEqThan = buildPtrOperGrEqThan()
    val ptrOperLessEqThan = buildPtrOperLessEqThan()



    init {
        primitives(allPrimitives)

        mathModule()

        if (errorHandler.hasErrors)
            println(errorHandler.formatErrors(null))
    }
}
