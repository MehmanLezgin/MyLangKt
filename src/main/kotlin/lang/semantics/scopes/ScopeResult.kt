package lang.semantics.scopes

import lang.semantics.symbols.Symbol
import lang.semantics.types.Type
import lang.infrastructure.operators.OperatorType
import lang.messages.Terms
import lang.semantics.symbols.CallableSymbol
import lang.semantics.symbols.FuncKind
import lang.semantics.types.TemplateArg

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

    data class NotDefined(
        val itemKind: String = Terms.SYMBOL,
        val symName: String,
        val scopeName: String?
    ) : ScopeError()

    data class NoFuncOverload(
        val symName: String,
        val kind: FuncKind,
        val argTypes: List<Type>,
        val scopeName: String?,
        val templateArgs: List<TemplateArg>?,
    ) : ScopeError()

    data class NoImplicitConversion(
        val fromType: Type,
        val toType: Type
    ) : ScopeError()

    data class Inaccessible(val symName: String) : ScopeError()

    object ConflictingOverloads : ScopeError()
    object InvalidConstValue : ScopeError()
    data class AmbiguousOverloadedFunc(val list: List<CallableSymbol>) : ScopeError()
    object ExpectedName : ScopeError()
}