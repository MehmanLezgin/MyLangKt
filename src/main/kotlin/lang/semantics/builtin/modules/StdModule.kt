package lang.semantics.builtin.modules

import lang.semantics.builtin.PrimitivesScope.constCharPtr
import lang.semantics.builtin.PrimitivesScope.uint64
import lang.semantics.builtin.PrimitivesScope.voidPtr
import lang.semantics.builtin.builders.func
import lang.semantics.builtin.builders.module
import lang.semantics.symbols.ModuleSymbol

fun stdModule(): ModuleSymbol {
    return module(name = "std") {
        staticScope {
            func(name = "println") {
                params {
                    "str" ofType constCharPtr
                }
            }

            func(name = "alloc") {
                params { "size" ofType uint64 }
                returns(voidPtr)
            }

            func(name = "free") {
                params { "ptr" ofType voidPtr }
            }
        }
    }
}