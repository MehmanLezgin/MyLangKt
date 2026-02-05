package lang.mappers

import lang.messages.Msg
import lang.semantics.scopes.ScopeError

object ScopeErrorMapper : IOneWayMapper<ScopeError, String> {
    private fun operatorParamMsg(operSymbolStr: String, expected: Int): String {
        return when (expected) {
            0 -> Msg.F_OPERATOR_REQUIRES_NO_PARAMS
            1 -> Msg.F_OPERATOR_REQUIRES_1_PARAM
            else -> Msg.F_OPERATOR_REQUIRES_X_PARAMS
        }.format(Msg.NON_STATIC, operSymbolStr, expected)
    }

    override fun toSecond(a: ScopeError): String {
        return when (a) {
            is ScopeError.AlreadyDefined ->
                Msg.F_SYMBOL_ALREADY_DEFINED.format(a.symName)

            is ScopeError.OperParamCountMismatch ->
                operatorParamMsg(a.oper.symbol, a.expected)

            is ScopeError.NotDefined ->
                Msg.F_SYMBOL_NOT_DEFINED_IN.format(a.symName, a.scopeName)

            ScopeError.AmbiguousOverloadedFunc -> Msg.AMBIGUOUS_OVERLOADED_FUNCTION
            ScopeError.CannotExport -> Msg.CANNOT_EXPORT
            ScopeError.ConflictingOverloads -> Msg.CONFLICTING_OVERLOADS
            ScopeError.InvalidConstValue -> Msg.INVALID_CONST_VALUE
            is ScopeError.NoFuncOverload -> {
                val msg = if (a.isOperator)
                    Msg.F_NO_OPER_OVERLOAD
                else
                    Msg.F_NO_FUNC_OVERLOAD

                msg.format(
                    a.symName,
                    a.argTypes.joinToString(", "),
                    a.scopeName,
                )
            }

            ScopeError.Redeclaration -> Msg.REDECLARATION
        }
    }
}