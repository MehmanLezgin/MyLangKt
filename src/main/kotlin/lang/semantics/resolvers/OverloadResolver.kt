package lang.semantics.resolvers

import lang.core.operators.OperatorType
import lang.messages.Terms
import lang.semantics.SemanticAnalyzer
import lang.semantics.builtin.PrimitivesScope.err
import lang.semantics.builtin.PrimitivesScope.ok
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeError
import lang.semantics.scopes.ScopeResult
import lang.semantics.symbols.FuncKind
import lang.semantics.symbols.FuncSymbol
import lang.semantics.symbols.OverloadedFuncSymbol
import lang.semantics.symbols.Symbol
import lang.semantics.types.Type
import kotlin.math.abs

class OverloadResolver(
    override val analyzer: SemanticAnalyzer,
    val convertResolver: ConvertResolver
) : BaseResolver<List<FuncSymbol>?, ScopeResult>(
    analyzer = analyzer,
) {
    override fun resolve(target: List<FuncSymbol>?): ScopeResult =
        error("Not Implemented")

    fun resolveBestOverloads(
        overloads: List<FuncSymbol>,
        from: Scope,
        argTypes: List<Type>
    ): List<FuncSymbol> {
        val ranks = overloads.map { func ->
            if (func.params.list.size != argTypes.size) {
                val sizeDiff = abs(func.params.list.size - argTypes.size)
                val cost = sizeDiff  * 1000
                return@map func to cost
            }

            val params = func.params.list
            var totalCost = 0

            if (!scope.isSymAccessibleFrom(func, from))
                totalCost += Int.MAX_VALUE

            for (i in argTypes.indices) {
                val argType = argTypes[i]
                val paramType = params[i].type

                val cost = convertResolver.conversionCost(argType, paramType)
//                if (conversion.notExists()) return@mapNotNull null
                totalCost += cost
            }

            func to totalCost
        }

        val minCost = ranks.minOfOrNull { it.second } ?: return emptyList()

        return ranks.filter { it.second == minCost }.map { it.first }
    }

    private fun pickSingleFuncSym(
        name: String,
        from: Scope,
        argTypes: List<Type>,
        kind: FuncKind,
        overloads: List<FuncSymbol>?
    ): ScopeResult {
        return if (overloads.isNullOrEmpty())
            ScopeError.NoFuncOverload(
                kind = kind,
                symName = name,
                argTypes = argTypes,
                scopeName = scope.absoluteScopePath
            ).err()
        else if (overloads.size > 1) {
            val accessibleOverloads = filterIsAccessible(list = overloads, from = from, asMember = false)

            if (accessibleOverloads.size > 1)
                return ScopeError.AmbiguousOverloadedFunc(list = accessibleOverloads).err()

            pickSingleFuncSym(
                name = name,
                from = from,
                argTypes = argTypes,
                kind = kind,
                overloads = accessibleOverloads
            )
        } else {
            val best = overloads.first()

            if (!scope.isSymAccessibleFrom(sym = best, from = from, asMember = false))
                return ScopeError.Inaccessible(best.name).err()

            best.ok()
        }
    }

    fun <T : Symbol> filterIsAccessible(list: List<T>, from: Scope, asMember: Boolean = false): List<T> {
        return list.filter { sym ->
            scope.isSymAccessibleFrom(
                sym = sym,
                from = from,
                asMember = asMember
            )
        }
    }

    fun resolveFunc(
        name: String,
        kind: FuncKind,
        from: Scope,
        argTypes: List<Type>,
        isStatic: Boolean = false
    ): ScopeResult {
        return when (val result = scope.resolve(name, from, !isStatic)) {
            is ScopeResult.ResultList,
            is ScopeResult.Error -> result

            is ScopeResult.Success<*> -> {
                val overloads = when (val sym = result.sym) {
                    is OverloadedFuncSymbol -> {
                        val effectiveArgTypes =
                            if (isStatic) argTypes else argTypes.drop(1)

                        val bestOverloads = resolveBestOverloads(
                            overloads = sym.overloads,
                            from = from,
                            argTypes = effectiveArgTypes
                        )

                        if (bestOverloads.isEmpty())
                            return ScopeError.NoFuncOverload(
                                symName = name,
                                kind = sym.kind,
                                argTypes = argTypes,
                                scopeName = scope.absoluteScopePath
                            ).err()

                        bestOverloads
                    }

                    else -> return ScopeError.NotDefined(
                        symName = name,
                        scopeName = scope.absoluteScopePath
                    ).err()
                }

                pickSingleFuncSym(
                    name = name,
                    from = from,
                    argTypes = argTypes,
                    kind = kind,
                    overloads = overloads
                )
            }
        }
    }

    fun resolveFunc(
        overloadedFunc: OverloadedFuncSymbol,
        from: Scope,
        argTypes: List<Type>,
    ): ScopeResult {
        val costOverloads = resolveBestOverloads(
            overloadedFunc.overloads,
            from,
            argTypes
        )

        return pickSingleFuncSym(
            name = overloadedFunc.name,
            from = from,
            argTypes = argTypes,
            kind = overloadedFunc.kind,
            overloads = costOverloads
        )
    }

    fun resolveOperFunc(
        operator: OperatorType,
        from: Scope,
        argTypes: List<Type>,
        isStatic: Boolean
    ): ScopeResult =
        resolveFunc(
            name = operator.fullName,
            kind = FuncKind.OPERATOR,
            from = from,
            argTypes = argTypes,
            isStatic = isStatic
        )

    fun resolveConstructor(
        argTypes: List<Type>,
        from: Scope
    ): ScopeResult {
        val overloadedFunc = (scope.symbols.values.find { sym ->
            sym is OverloadedFuncSymbol && sym.kind == FuncKind.CONSTRUCTOR
        } as? OverloadedFuncSymbol)
            ?: return ScopeError.NoFuncOverload(
                symName = Terms.CONSTRUCTOR,
                kind = FuncKind.CONSTRUCTOR,
                argTypes = argTypes,
                scopeName = scope.absoluteScopePath
            ).err()

        return resolveFunc(
            overloadedFunc = overloadedFunc,
            from = from,
            argTypes = argTypes
        )
    }
}