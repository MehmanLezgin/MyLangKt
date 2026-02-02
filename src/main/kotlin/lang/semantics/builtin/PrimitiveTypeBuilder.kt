package lang.semantics.builtin

import lang.semantics.builtin.PrimitivesScope.voidPtr
import lang.semantics.builtin.builders.ConstVarBuilder
import lang.semantics.builtin.builders.FuncBuilder
import lang.semantics.builtin.builders.ModuleBuilder
import lang.semantics.scopes.Scope
import lang.semantics.symbols.BuiltInOperatorFuncSymbol
import lang.semantics.symbols.ClassSymbol
import lang.semantics.symbols.FuncParamListSymbol
import lang.semantics.symbols.FuncParamSymbol
import lang.semantics.symbols.FuncSymbol
import lang.semantics.symbols.TypeSymbol
import lang.semantics.types.ConstValue
import lang.semantics.types.PrimitiveType
import lang.semantics.types.Type
import lang.tokens.OperatorType

//fun PrimitiveType.toConst() =
//    this.setFlags(isConst = true) as PrimitiveType

fun createOperFunc(
    operator: OperatorType,
    returnType: Type = PrimitivesScope.void,
    vararg paramTypes: Type
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

    return func
}

fun createFunc(
    name: String,
    returnType: Type = PrimitivesScope.void,
    vararg paramTypes: Pair<String, Type>
): FuncSymbol {
    val paramList = FuncParamListSymbol(list = paramTypes.map { (paramName, paramType) ->
        FuncParamSymbol(
            name = paramName,
            type = paramType,
            defaultValue = null
        )
    })

    val func = FuncSymbol(
        name = name,
        params = paramList,
        returnType = returnType
    )

    return func
}

fun PrimitivesScope.createBinOper(operator: OperatorType, type: Type) {
    createOperFunc(operator, returnType = type, type, type)
}

fun PrimitivesScope.createBinOpers(type: Type, vararg operators: OperatorType) {
    operators.forEach { operator -> createBinOper(operator, type) }
}

fun Type.operFunc(name: String, block: FuncBuilder.() -> Unit): Type {
    val sym = FuncBuilder(name).apply(block).build()
    declaration?.withInstanceScope { defineFunc(sym, null) }
    return this
}

fun TypeSymbol.staticFunc(name: String, block: FuncBuilder.() -> Unit): TypeSymbol {
    val sym = FuncBuilder(name).apply(block).build()
    withStaticScope { defineFunc(sym, null) }
    return this
}

fun Type.operFunc(oper: OperatorType, block: FuncBuilder.() -> Unit): Type {
    val builder = FuncBuilder(oper)

    builder.params { "t" ofType this@operFunc }

    val sym = builder.apply(block).build()
    declaration?.withStaticScope { defineFunc(sym, null) }
    return this
}

fun PrimitivesScope.globalOperFunc(oper: OperatorType, block: FuncBuilder.() -> Unit): FuncSymbol {
    val builder = FuncBuilder(oper)
    val sym = builder.apply(block).build()
    defineFunc(sym, null)
    return sym
}

fun PrimitivesScope.module(name: String, block: ModuleBuilder.() -> Unit): ClassSymbol {
    val sym = ModuleBuilder(name, this).apply(block).build()
    define(sym, null)
    return sym
}

fun Type.staticConstVar(
    name: String,
    type: Type,
    value: ConstValue<*>,
    block: ConstVarBuilder.() -> Unit = {}
): Type {
    val sym = ConstVarBuilder(name, type, value).apply(block).build()
    declaration?.withStaticScope { define(sym, null) }
    return this
}

fun TypeSymbol.withStaticScope(block: Scope.() -> Unit) {
    this.staticScope.apply(block)
}

fun TypeSymbol.withInstanceScope(block: Scope.() -> Unit) {
    this.staticScope.instanceScope.apply(block)
}

fun FuncSymbol.isBuiltInFuncReturnsPtr() =
    this is BuiltInOperatorFuncSymbol &&
            this.returnType == voidPtr

fun PrimitivesScope.primitives(list: List<PrimitiveType>) {
    list.forEach { primitiveType ->
        primitiveType.initWith(scope = this)
    }
}
