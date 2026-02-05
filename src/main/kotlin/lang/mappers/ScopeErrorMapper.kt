package lang.mappers

import lang.messages.Msg
import lang.messages.Terms
import lang.messages.Terms.exactly
import lang.messages.Terms.plural
import lang.semantics.scopes.ScopeError

object ScopeErrorMapper : IOneWayMapper<ScopeError, String> {
    private fun operatorParamMsg(operSymbolStr: String, expected: Int): String {
        val expectedStr = "${expected.exactly()} ${Terms.PARAM.plural()}"

        return Msg.SymRequiresItem.format(
            "${Terms.NON_STATIC} ${Terms.OPERATOR}",
            operSymbolStr,
            expectedStr
        )
    }

    override fun toSecond(a: ScopeError): String {
        return when (a) {
            is ScopeError.AlreadyDefined ->
                Msg.SymbolAlreadyDefinedIn.format(
                    name = a.symName,
                    scopeName = a.scopeName
                )

            is ScopeError.OperParamCountMismatch ->
                operatorParamMsg(a.oper.symbol, a.expected)

            is ScopeError.NotDefined ->
                Msg.SymbolNotDefinedIn.format(name = a.symName, scopeName = a.scopeName)

            ScopeError.AmbiguousOverloadedFunc -> Msg.AMBIGUOUS_OVERLOADED_FUNCTION
            ScopeError.CannotExport -> Msg.CANNOT_EXPORT
            ScopeError.ConflictingOverloads -> Msg.CONFLICTING_OVERLOADS
            ScopeError.InvalidConstValue -> Msg.INVALID_CONST_VALUE
            is ScopeError.NoFuncOverload -> {
                val msg = if (a.isOperator)
                    Msg.NoOperOverload
                else Msg.NoFuncOverload

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