package lang.semantics.types

import lang.messages.ErrorHandler
import lang.semantics.scopes.Scope
import lang.semantics.symbols.BuiltInOperatorFuncSymbol
import lang.semantics.symbols.FuncParamListSymbol
import lang.semantics.symbols.FuncParamSymbol
import lang.semantics.symbols.PrimitiveTypeSymbol
import lang.semantics.symbols.TypedefSymbol
import lang.tokens.OperatorType
import lang.tokens.Pos

object BuiltInTypes {

    private val allPrimitiveTypes = mutableListOf<PrimitiveType>()
    private val allFuncOperators = mutableListOf<BuiltInOperatorFuncSymbol>()

//    private val allBuiltInSymbols = mutableListOf<Symbol>()

    private fun createPrimitiveType(
        name: String,
        size: PrimitiveSize,
        prec: Int,
        isConst: Boolean = false
    ): PrimitiveType {
        val type = PrimitiveType(
            name = name,
            size = size,
            prec = prec,
            flags = TypeFlags(
                isConst = isConst,
                isExprType = false
            )
        )

        allPrimitiveTypes.add(type)
        return type
    }

    private fun createOperFunc(
        operator: OperatorType,
        returnType: Type = void,
        vararg paramTypes: Type,
    ): BuiltInOperatorFuncSymbol {
        val paramList = FuncParamListSymbol(list = paramTypes.mapIndexed { index, type ->
            FuncParamSymbol(
                name = "x$index",
                type = type,
                defaultValue = null
            )
        })

        val func = BuiltInOperatorFuncSymbol(
            operator = operator,
            params = paramList,
            returnType = returnType
        )

        allFuncOperators.add(func)
        return func
    }

    // ---- BOOL ----
    val bool = createPrimitiveType("bool", PrimitiveSize.BYTE, prec = 0)
    val boolConst = createPrimitiveType("bool", PrimitiveSize.BYTE, prec = 0, isConst = true)

    // ---- CHAR ----
    val char = createPrimitiveType("char", PrimitiveSize.BYTE, prec = 1)
    val uchar = createPrimitiveType("uchar", PrimitiveSize.BYTE, prec = 1)

    val charConst = createPrimitiveType("char", PrimitiveSize.BYTE, prec = 1, isConst = true)
    val ucharConst = createPrimitiveType("uchar", PrimitiveSize.BYTE, prec = 1, isConst = true)

    // ---- 8 BIT ----
    val int8 = createPrimitiveType("byte", PrimitiveSize.BYTE, prec = 2)
    val uint8 = createPrimitiveType("ubyte", PrimitiveSize.BYTE, prec = 3)

    val int8Const = createPrimitiveType("byte", PrimitiveSize.BYTE, prec = 2, isConst = true)
    val uint8Const = createPrimitiveType("ubyte", PrimitiveSize.BYTE, prec = 3, isConst = true)

    // ---- 16 BIT ----
    val int16 = createPrimitiveType("short", PrimitiveSize.WORD, prec = 4)
    val uint16 = createPrimitiveType("ushort", PrimitiveSize.WORD, prec = 5)

    val int16Const = createPrimitiveType("short", PrimitiveSize.WORD, prec = 4, isConst = true)
    val uint16Const = createPrimitiveType("ushort", PrimitiveSize.WORD, prec = 5, isConst = true)

    // ---- 32 BIT ----
    val int32 = createPrimitiveType("int", PrimitiveSize.DWORD, prec = 6)
    val uint32 = createPrimitiveType("uint", PrimitiveSize.DWORD, prec = 7)

    val int32Const = createPrimitiveType("int", PrimitiveSize.DWORD, prec = 6, isConst = true)
    val uint32Const = createPrimitiveType("uint", PrimitiveSize.DWORD, prec = 7, isConst = true)

    // ---- 64 BIT ----
    val int64 = createPrimitiveType("long", PrimitiveSize.QWORD, prec = 8)
    val uint64 = createPrimitiveType("ulong", PrimitiveSize.QWORD, prec = 9)

    val int64Const = createPrimitiveType("long", PrimitiveSize.QWORD, prec = 8, isConst = true)
    val uint64Const = createPrimitiveType("ulong", PrimitiveSize.QWORD, prec = 9, isConst = true)

    // ---- FLOAT ----
    val float32 = createPrimitiveType("float", PrimitiveSize.DWORD, prec = 10)
    val float64 = createPrimitiveType("double", PrimitiveSize.QWORD, prec = 11)

    val float32Const = createPrimitiveType("float", PrimitiveSize.DWORD, prec = 10, isConst = true)
    val float64Const = createPrimitiveType("double", PrimitiveSize.QWORD, prec = 11, isConst = true)

    val void = createPrimitiveType("void", PrimitiveSize.NO_SIZE, prec = Int.MAX_VALUE, isConst = false)

    val voidPtr = PointerType(base = void, level = 1)
    val charPtr = PointerType(base = void, level = 1)

    private val ARITHMETIC_OPS = arrayOf(
        OperatorType.PLUS,
        OperatorType.MINUS,
        OperatorType.MUL,
        OperatorType.DIV,
        OperatorType.REMAINDER,

        OperatorType.PLUS_ASSIGN,
        OperatorType.MINUS_ASSIGN,
        OperatorType.MUL_ASSIGN,
        OperatorType.DIV_ASSIGN,
        OperatorType.REMAINDER_ASSIGN,
    )

    private val SHIFT_OPS = arrayOf(
        OperatorType.SHIFT_LEFT,
        OperatorType.SHIFT_RIGHT,

        OperatorType.SHIFT_LEFT_ASSIGN,
        OperatorType.SHIFT_RIGHT_ASSIGN,
    )

    private val BITWISE_OPS = arrayOf(
        OperatorType.AMPERSAND,
        OperatorType.BIN_OR,
        OperatorType.BIN_XOR,

        OperatorType.BIN_AND_ASSIGN,
        OperatorType.BIN_OR_ASSIGN,
        OperatorType.BIN_XOR_ASSIGN
    )

    private val LOGICAL_OPS = arrayOf(
        OperatorType.AND,
        OperatorType.OR
    )

    private val COMPARE_OPS = arrayOf(
        OperatorType.LESS,
        OperatorType.LESS_EQUAL,
        OperatorType.GREATER,
        OperatorType.GREATER_EQUAL,
        OperatorType.EQUAL,
        OperatorType.NOT_EQUAL
    )

//    private val ASSIGN_OPS = arrayOf(
//        OperatorType.LESS,
//        OperatorType.LESS_EQUAL,
//        OperatorType.GREATER,
//        OperatorType.GREATER_EQUAL,
//        OperatorType.EQUAL,
//        OperatorType.NOT_EQUAL,
//    )


    init {
        fun createBinOper(operator: OperatorType, type: Type) {
            createOperFunc(operator, returnType = type, type, type)
        }

        fun createBinOpers(type: Type, vararg operators: OperatorType) {
            operators.forEach { operator -> createBinOper(operator, type) }
        }

        val signedInts = listOf(int8, int16, int32, int64)
        val unsignedInts = listOf(uint8, uint16, uint32, uint64)
        val floats = listOf(float32, float64)
        val allInts = signedInts + unsignedInts

        // ---- INTEGER OPERATORS ----
        allInts.forEach { t ->
            createBinOpers(t, *ARITHMETIC_OPS)
            createBinOpers(t, *SHIFT_OPS)
            createBinOpers(t, *BITWISE_OPS)
        }

        // ---- FLOAT / DOUBLE ----
        floats.forEach { t ->
            createBinOpers(
                t,
                OperatorType.PLUS,
                OperatorType.MINUS,
                OperatorType.MUL,
                OperatorType.DIV
            )
        }

        // ---- COMPARISONS (ALL NUMERIC + CHAR, ONCE) ----
        (allInts + floats + char).forEach { t ->
            COMPARE_OPS.forEach { op ->
                createOperFunc(op, returnType = bool, t, t)
            }
        }


        // ---- CHAR ARITHMETIC ONLY (NO COMPARISONS HERE) ----
        createBinOpers(
            char,
            OperatorType.PLUS,
            OperatorType.MINUS
        )

        // ---- BOOLEAN ----
        createBinOpers(bool, *LOGICAL_OPS)
        createBinOpers(bool, OperatorType.EQUAL, OperatorType.NOT_EQUAL)

        // ---- OTHERS ----
        createBinOper(OperatorType.ELVIS, voidPtr)


        // ---- POINTERS ----
        createOperFunc(OperatorType.PLUS, returnType = voidPtr, voidPtr, int32)
        createOperFunc(OperatorType.MINUS, returnType = voidPtr, voidPtr, int32)
        createOperFunc(OperatorType.EQUAL, returnType = voidPtr, voidPtr, voidPtr)
        createOperFunc(OperatorType.NOT_EQUAL, returnType = voidPtr, voidPtr, voidPtr)
        createOperFunc(OperatorType.LESS, returnType = voidPtr, voidPtr, voidPtr)
        createOperFunc(OperatorType.LESS_EQUAL, returnType = voidPtr, voidPtr, voidPtr)
        createOperFunc(OperatorType.GREATER, returnType = voidPtr, voidPtr, voidPtr)
        createOperFunc(OperatorType.GREATER_EQUAL, returnType = voidPtr, voidPtr, voidPtr)
    }

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
        is String -> charPtr
        else -> null
    }

    fun highest(a: PrimitiveType, b: PrimitiveType) = if (a.prec >= b.prec) a else b

    /*fun PrimitiveType.common(other: PrimitiveType): PrimitiveType {
        val result = if (this.prec > other.prec) this else other
        val isConst = this.isConst || other.isConst
        return PrimitiveType(
            name = result.name,
            size = result.size,
            prec= result.prec,
            flags = TypeFlags(
                isConst = isConst,
                isExprType = false
            )
        )
    }*/

    /*fun createTypedef(
        scope: Scope,
        type: Type,
        vararg names: String,
    ) {
        val pos = Pos()
        names.forEach { name ->
            val sym = TypedefSymbol(name = name, type = type)
            scope.define(sym = sym, pos = pos)
        }
    }*/

    fun initBuiltInTypes(
        scope: Scope,
        errorHandler: ErrorHandler
    ) {
        val pos = Pos()

        allPrimitiveTypes.forEach { type ->
            if (type.isConst) return@forEach

            val sym = PrimitiveTypeSymbol(
                type = type,
                scope = Scope(
                    parent = scope,
                    errorHandler = errorHandler
                )
            )
            scope.define(
                sym = sym,
                pos = pos
            )
        }

        allFuncOperators.forEach { funcSym ->
            scope.defineFunc(
                funcSym = funcSym,
                pos = pos
            )
        }
    }

}