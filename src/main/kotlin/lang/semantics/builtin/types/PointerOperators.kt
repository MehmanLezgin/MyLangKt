package lang.semantics.builtin.types

import lang.semantics.builtin.PrimitivesScope
import lang.semantics.builtin.PrimitivesScope.bool
import lang.semantics.builtin.PrimitivesScope.voidPtr
import lang.semantics.builtin.builders.FuncBuilder
import lang.semantics.builtin.globalOperFunc
import lang.semantics.symbols.BuiltInOperatorFuncSymbol
import lang.core.operators.OperatorType

fun PrimitivesScope.buildPtrOperPlus() =
    globalOperFunc(OperatorType.PLUS) {
        params {
            "ptr" ofType voidPtr
            "offset" ofType int32
        }
        returns(voidPtr)
    } as BuiltInOperatorFuncSymbol

fun PrimitivesScope.buildPtrOperMinus() =
    globalOperFunc(OperatorType.MINUS) {
        params {
            "ptr" ofType voidPtr
            "offset" ofType int32
        }
        returns(voidPtr)
    } as BuiltInOperatorFuncSymbol


fun PrimitivesScope.buildPtrOperEq() =
    globalOperFunc(OperatorType.EQUAL) {
        ptrComparison()
    } as BuiltInOperatorFuncSymbol

fun PrimitivesScope.buildPtrOperNotEq() =
    globalOperFunc(OperatorType.NOT_EQUAL) {
        ptrComparison()
    } as BuiltInOperatorFuncSymbol

fun PrimitivesScope.buildPtrOperGrThan() =
    globalOperFunc(OperatorType.GREATER) {
        ptrComparison()
    } as BuiltInOperatorFuncSymbol

fun PrimitivesScope.buildPtrOperLessThan() =
    globalOperFunc(OperatorType.LESS) {
        ptrComparison()
    } as BuiltInOperatorFuncSymbol

fun PrimitivesScope.buildPtrOperGrEqThan() =
    globalOperFunc(OperatorType.GREATER_EQUAL) {
        ptrComparison()
    } as BuiltInOperatorFuncSymbol

fun PrimitivesScope.buildPtrOperLessEqThan() =
    globalOperFunc(OperatorType.LESS_EQUAL) {
        ptrComparison()
    } as BuiltInOperatorFuncSymbol


fun FuncBuilder.ptrComparison() {
    params {
        "a" ofType voidPtr
        "b" ofType voidPtr
    }
    returns(bool)
}
