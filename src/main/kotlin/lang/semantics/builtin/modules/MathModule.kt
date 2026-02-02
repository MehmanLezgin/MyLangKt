package lang.semantics.builtin.modules

import lang.semantics.builtin.PrimitivesScope
import lang.semantics.builtin.module
import lang.semantics.types.ConstValue

fun PrimitivesScope.mathModule() {
    module("Math") {
        staticConstVar("PI", float64Const, ConstValue(Math.PI, float64Const))

        staticFunc("sin") {
            params { "a" ofType float64 }
            returns(float64)
        }

        staticFunc("cos") {
            params { "a" ofType float64 }
            returns(float64)
        }

        staticFunc("tan") {
            params { "a" ofType float64 }
            returns(float64)
        }

        staticFunc("sqrt") {
            params { "a" ofType float64 }
            returns(float64)
        }

        staticFunc("pow") {
            params {
                "a" ofType float64
                "b" ofType float64
            }
            returns(float64)
        }
    }

}