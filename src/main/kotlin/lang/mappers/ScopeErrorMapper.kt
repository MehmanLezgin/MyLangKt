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
                Msg.SymbolNotDefinedIn.format(itemKind = a.itemKind, name = a.symName, scopeName = a.scopeName)

            is ScopeError.AmbiguousOverloadedFunc ->
                Msg.AmbiguousOverloadedFunc.format(list = a.list)

            ScopeError.ConflictingOverloads -> Msg.CONFLICTING_OVERLOADS
            ScopeError.InvalidConstValue -> Msg.INVALID_CONST_VALUE

            is ScopeError.NoFuncOverload -> {
                Msg.NoFuncOverload.format(
                    kind = a.kind,
                    funcName = a.symName,
                    paramsStr = a.argTypes.joinToString(", "),
                    scopeName = a.scopeName,
                )
            }

            is ScopeError.Inaccessible ->
                Msg.SymbolIsInaccessible.format(name = a.symName)

            is ScopeError.ExpectedName -> Msg.FNameExpected.format(Terms.SYMBOL)

            is ScopeError.NoImplicitConversion ->
                Msg.NoImplicitConversion.format(
                    fromType = a.fromType.toString(),
                    toType = a.toType.toString()
                )
        }
    }
}