package lang.semantics.builtin.modules

import lang.semantics.builtin.PrimitivesScope
import lang.semantics.builtin.builders.constVar
import lang.semantics.builtin.builders.func
import lang.semantics.builtin.builders.module
import lang.semantics.symbols.ModuleSymbol

fun mathModule(): ModuleSymbol {
    return module(name = "math") {
        staticScope {
            constVar(
                name = "E",
                type = PrimitivesScope.float64,
                value = Math.E
            )

            constVar(
                name = "PI",
                type = PrimitivesScope.float64,
                value = Math.PI
            )

            func(name = "sin") {
                params { PrimitivesScope.float64.param }
                returns(PrimitivesScope.float64)
            }

            func(name = "cos") {
                params { PrimitivesScope.float64.param }
                returns(PrimitivesScope.float64)
            }

            func(name = "tan") {
                params { PrimitivesScope.float64.param }
                returns(PrimitivesScope.float64)
            }

            func(name = "sqrt") {
                params { PrimitivesScope.float64 }
                returns(PrimitivesScope.float64)
            }

            func(name = "pow") {
                params {
                    PrimitivesScope.float64.param
                    PrimitivesScope.float64.param
                }
                returns(PrimitivesScope.float64)
            }


        }
    }
}