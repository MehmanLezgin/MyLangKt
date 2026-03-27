package lang.semantics.scopes

import lang.semantics.symbols.Symbol
import lang.semantics.types.Type
import lang.core.operators.OperatorType
import lang.semantics.symbols.FuncKind

sealed class ScopeResult {
    data class Success<T : Symbol>(val sym: T) : ScopeResult()
    data class Error(val error: ScopeError) : ScopeResult()
    data class ResultList(val list: List<ScopeResult>) : ScopeResult()
}

sealed class ScopeError {
    data class AlreadyDefined(val symName: String, val scopeName: String?) : ScopeError()

    data class OperParamCountMismatch(
        val oper: OperatorType,
        val expected: Int,
        val isStatic: Boolean = false
    ) : ScopeError()

    data class NotDefined(val symName: String, val scopeName: String?) : ScopeError()

    data class NoFuncOverload(
        val symName: String,
        val kind: FuncKind,
        val argTypes: List<Type>,
        val scopeName: String?,
    ) : ScopeError()

    data class Inaccessible(val symName: String) : ScopeError()

    object ConflictingOverloads : ScopeError()
    object InvalidConstValue : ScopeError()
    object AmbiguousOverloadedFunc : ScopeError()
}