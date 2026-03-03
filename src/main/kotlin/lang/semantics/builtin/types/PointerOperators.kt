package lang.semantics.builtin.types

import lang.semantics.builtin.PrimitivesScope
import lang.semantics.builtin.PrimitivesScope.bool
import lang.semantics.builtin.PrimitivesScope.voidPtr
import lang.semantics.builtin.builders.FuncBuilder
import lang.semantics.symbols.BuiltInOperatorFuncSymbol
import lang.core.operators.OperatorType
import lang.semantics.builtin.builders.init
import lang.semantics.builtin.builders.operFunc

fun PrimitivesScope.ptrOperPlus() =
    init {
        operFunc(OperatorType.PLUS) {
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
            ptrComparison()
        } as BuiltInOperatorFuncSymbol
    }

fun PrimitivesScope.ptrOperNotEq() =
    init {
        operFunc(OperatorType.NOT_EQUAL) {
            ptrComparison()
        } as BuiltInOperatorFuncSymbol
    }

fun PrimitivesScope.ptrOperGrThan() =
    init {
        operFunc(OperatorType.GREATER) {
            ptrComparison()
        } as BuiltInOperatorFuncSymbol
    }

fun PrimitivesScope.ptrOperLessThan() =
    init {
        operFunc(OperatorType.LESS) {
            ptrComparison()
        } as BuiltInOperatorFuncSymbol
    }

fun PrimitivesScope.ptrOperGrEqThan() =
    init {
        operFunc(OperatorType.GREATER_EQUAL) {
            ptrComparison()
        } as BuiltInOperatorFuncSymbol
    }

fun PrimitivesScope.ptrOperLessEqThan() =
    init {
        operFunc(OperatorType.LESS_EQUAL) {
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
