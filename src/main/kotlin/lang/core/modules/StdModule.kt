package lang.core.modules

import lang.core.PrimitivesScope.constCharPtr
import lang.core.PrimitivesScope.uint64
import lang.core.PrimitivesScope.voidPtr
import lang.core.builders.func
import lang.core.builders.module
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