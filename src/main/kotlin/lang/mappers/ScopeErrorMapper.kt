package lang.mappers

import lang.messages.Msg
import lang.messages.Terms
import lang.messages.Terms.exactly
import lang.messages.Terms.plural
import lang.semantics.scopes.ScopeError

object ScopeErrorMapper : IOneWayMapper<ScopeError, String> {
    private fun operatorParamMsg(operSymbolStr: String, expected: Int, isStatic: Boolean): String {
        val expectedStr = "${expected.exactly()} ${Terms.PARAM.plural()}"
        val prefix = if (isStatic) Terms.STATIC else Terms.NON_STATIC

        return Msg.SymRequiresItem.format(
            "$prefix ${Terms.OPERATOR}",
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
                operatorParamMsg(a.oper.raw, a.expected, a.isStatic)

            is ScopeError.NotDefined ->
                Msg.SymbolNotDefinedIn.format(name = a.symName, scopeName = a.scopeName)

            ScopeError.AmbiguousOverloadedFunc -> Msg.AMBIGUOUS_OVERLOADED_FUNCTION
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

            is ScopeError.Inaccessible ->
                Msg.SymbolIsInaccessible.format(name = a.symName)
        }
    }
}