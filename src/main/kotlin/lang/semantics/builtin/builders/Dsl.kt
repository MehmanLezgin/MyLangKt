package lang.semantics.builtin.builders

import lang.core.operators.OperatorType
import lang.messages.Terms
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.types.Type

fun module(name: String, block: ModuleBuilder.() -> Unit): ModuleSymbol {
    return ModuleBuilder(name).apply(block).build()
}

fun <T : Scope> T.init(block: ScopeBuilder.() -> Unit): Scope {
    ScopeBuilder(this).apply(block)
    return this
}

fun BaseTypeScope.init(block: TypeScopeBuilder.() -> Unit): Scope {
    TypeScopeBuilder(this).apply(block)
    return this
}

fun ScopeBuilder.func(name: String, block: FuncBuilder.() -> Unit) =
    addFunc(name, block)

fun ScopeBuilder.operFunc(oper: OperatorType, block: FuncBuilder.() -> Unit) =
    addOperFunc(oper, block)

fun TypeScopeBuilder.constructor(block: FuncBuilder.() -> Unit) =
    addConstructor(block)

fun TypeScopeBuilder.implicitCast(type: Type) =
    addConstructor {
        params { Terms.VALUE ofType type }
    }

fun TypeScopeBuilder.registerImplicitCasts() {
    PrimitivesScope.convertiblePrimitives.forEach { target ->
        if (target != this)
            implicitCast(target)
    }
}


fun <T : Any> ScopeBuilder.constVar(
    name: String,
    type: Type,
    value: T,
    block: ConstVarBuilder.() -> Unit = {}
) =
    addConstVar(name, type, value, block)