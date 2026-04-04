package lang.core.types

import lang.core.PrimitivesScope
import lang.core.PrimitivesScope.bool
import lang.core.PrimitivesScope.voidPtr
import lang.core.builders.FuncBuilder
import lang.semantics.symbols.BuiltInOperatorFuncSymbol
import lang.infrastructure.operators.OperatorType
import lang.core.builders.init
import lang.core.builders.operFunc
import lang.semantics.symbols.Modifiers

fun PrimitivesScope.ptrOperPlus() =
    init {
        operFunc(OperatorType.PLUS) {
            modifiers(Modifiers(isStatic = true))

            params {
                "ptr" ofType voidPtr
                "offset" ofType int32
            }

            returns(voidPtr)
        } as BuiltInOperatorFuncSymbol
    }

fun PrimitivesScope.ptrOperMinus() =
    init {
        operFunc(OperatorType.MINUS) {
            modifiers(Modifiers(isStatic = true))

            params {
                "ptr" ofType voidPtr
                "offset" ofType int32
            }
            returns(voidPtr)
        } as BuiltInOperatorFuncSymbol
    }

fun PrimitivesScope.ptrOperEq() =
    init {
        operFunc(OperatorType.EQUAL) {
            modifiers(Modifiers(isStatic = true))

            ptrComparison()
        } as BuiltInOperatorFuncSymbol
    }

fun PrimitivesScope.ptrOperNotEq() =
    init {
        operFunc(OperatorType.NOT_EQUAL) {
            modifiers(Modifiers(isStatic = true))

            ptrComparison()
        } as BuiltInOperatorFuncSymbol
    }

fun PrimitivesScope.ptrOperGrThan() =
    init {
        operFunc(OperatorType.GREATER) {
            modifiers(Modifiers(isStatic = true))

            ptrComparison()
        } as BuiltInOperatorFuncSymbol
    }

fun PrimitivesScope.ptrOperLessThan() =
    init {
        operFunc(OperatorType.LESS) {
            modifiers(Modifiers(isStatic = true))

            ptrComparison()
        } as BuiltInOperatorFuncSymbol
    }

fun PrimitivesScope.ptrOperGrEqThan() =
    init {
        operFunc(OperatorType.GREATER_EQUAL) {
            modifiers(Modifiers(isStatic = true))

            ptrComparison()
        } as BuiltInOperatorFuncSymbol
    }

fun PrimitivesScope.ptrOperLessEqThan() =
    init {
        operFunc(OperatorType.LESS_EQUAL) {
            modifiers(Modifiers(isStatic = true))

            ptrComparison()
        } as BuiltInOperatorFuncSymbol
    }


fun FuncBuilder.ptrComparison() {
    params {
        "a" ofType voidPtr
        "b" ofType voidPtr
    }
    returns(bool)
}
