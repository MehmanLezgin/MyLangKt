package lang.semantics.scopes

import lang.semantics.symbols.Symbol
import lang.semantics.types.Type
import lang.core.operators.OperatorType

sealed class ScopeResult {
    data class Success<T : Symbol>(val sym: T) : ScopeResult()
    data class Error(val error: ScopeError) : ScopeResult()
}

sealed class ScopeError {
    data class AlreadyDefined(val symName: String, val scopeName: String?) : ScopeError()
    data class OperParamCountMismatch(val oper: OperatorType, val expected: Int) : ScopeError()
    data class NotDefined(val symName: String, val scopeName: String?) : ScopeError()
    data class NoFuncOverload(
        val symName: String,
        val isOperator: Boolean,
        val argTypes: List<Type>,
        val scopeName: String?,
    ) : ScopeError()

    object ConflictingOverloads : ScopeError()
    object Redeclaration : ScopeError()
    object InvalidConstValue : ScopeError()
    object AmbiguousOverloadedFunc : ScopeError()
    object CannotExport : ScopeError()
}