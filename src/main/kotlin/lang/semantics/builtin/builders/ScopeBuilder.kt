package lang.semantics.builtin.builders

import lang.core.operators.OperatorType
import lang.mappers.ScopeErrorMapper
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeResult
import lang.semantics.symbols.FuncSymbol
import lang.semantics.symbols.VarSymbol
import lang.semantics.types.ConstValue
import lang.semantics.types.Type

class ScopeBuilder(
    private val scope: Scope
) {
    fun build(): Scope = scope

    internal fun addFunc(name: String, block: FuncBuilder.() -> Unit): FuncSymbol {
        val sym = FuncBuilder(name).apply(block).build()
        scope.define(sym).handle()
        return sym
    }

    internal fun addOperFunc(oper: OperatorType, block: FuncBuilder.() -> Unit): FuncSymbol {
        val builder = FuncBuilder(oper)
        val sym = builder.apply(block).build()
        scope.define(sym).handle()
        return sym
    }

    internal fun <T : Any> addConstVar(
        name: String,
        type: Type,
        value: T,
        block: ConstVarBuilder.() -> Unit = {}
    ): VarSymbol {
        val sym = ConstVarBuilder(name, type, ConstValue(value, type)).apply(block).build()
        scope.define(sym).handle()
        return sym
    }

    private fun ScopeResult.handle() {
        when (this) {
            is ScopeResult.Success<*> -> return
            is ScopeResult.Error -> error(ScopeErrorMapper.toSecond(this.error))
            is ScopeResult.ResultList -> {
                list.forEach { item -> item.handle() }
            }
        }
    }
}