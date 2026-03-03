package lang.semantics.builtin

import lang.semantics.builtin.PrimitivesScope.voidPtr
import lang.semantics.symbols.BuiltInOperatorFuncSymbol
import lang.semantics.symbols.FuncSymbol
import lang.semantics.types.PrimitiveType

fun FuncSymbol.isBuiltInFuncReturnsPtr() =
    this is BuiltInOperatorFuncSymbol &&
            this.returnType == voidPtr

fun PrimitivesScope.primitives(list: List<PrimitiveType>) {
    list.forEach { primitiveType ->
        primitiveType.initWith(scope = this)
    }
}
