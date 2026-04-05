package lang.semantics.resolvers

import lang.core.PrimitivesScope.err
import lang.core.PrimitivesScope.ok
import lang.infrastructure.operators.OperatorType
import lang.messages.Terms
import lang.semantics.SemanticAnalyzer
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeError
import lang.semantics.scopes.ScopeResult
import lang.semantics.symbols.CallableSymbol
import lang.semantics.symbols.ConstructorSymbol
import lang.semantics.symbols.FuncKind
import lang.semantics.symbols.FuncSymbol
import lang.semantics.symbols.OverloadedFuncSymbol
import lang.semantics.symbols.Symbol
import lang.semantics.symbols.TemplateFuncSymbol
import lang.semantics.types.TemplateArg
import lang.semantics.types.Type

class OverloadResolver(
    override val analyzer: SemanticAnalyzer,
    val convertResolver: ConvertResolver
) : BaseResolver<List<FuncSymbol>?, ScopeResult>(
    analyzer = analyzer,
) {
    override fun resolve(target: List<FuncSymbol>?): ScopeResult =
        error("Not Implemented")

    private fun handleParamArgDiff(
        paramCount: Int,
        argCount: Int,
        costFactor: Int
    ): Int? {
        if (paramCount < argCount)
            return null

        if (paramCount > argCount) {
            val sizeDiff = paramCount - argCount
            val cost = sizeDiff * costFactor
            return cost
        }

        return 0
    }

    fun resolveBestOverloads(
        overloadedFuncSym: OverloadedFuncSymbol,
        from: Scope,
        argTypes: List<Type>,
        onlyImplicit: Boolean = false,
        templateArgs: List<TemplateArg>?,
    ): List<CallableSymbol> {
        val overloads = overloadedFuncSym.candidates

        val ranks = overloads.mapNotNull { func ->
            var totalCost = 0

            if (templateArgs?.isNotEmpty() == true) {
                if (func !is TemplateFuncSymbol)
                    return@mapNotNull func to Int.MAX_VALUE

                val paramCost = handleParamArgDiff(
                    paramCount = func.templateParams.size,
                    argCount = templateArgs.size,
                    costFactor = 1500
                )

                when {
                    paramCost == null -> return@mapNotNull null
                }

                totalCost += paramCost
            }

            val paramCost = handleParamArgDiff(
                paramCount = func.params.list.size,
                argCount = argTypes.size,
                costFactor = 1000
            )

            when {
                paramCost == null -> return@mapNotNull null
                paramCost > 0 -> return@mapNotNull func to paramCost
            }


            val params = func.params.list

            if (!scope.isSymAccessibleFrom(func, from))
                totalCost += 1000

            if (onlyImplicit && func is ConstructorSymbol && !func.modifiers.isImplicit)
                totalCost += 1000


            for (i in argTypes.indices) {
                val argType = argTypes[i]
                val paramType = params[i].type

                val cost = convertResolver.conversionCost(argType, paramType)
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
        overloads: List<CallableSymbol>,
        onlyImplicit: Boolean = false,
        templateArgs: List<TemplateArg>?
    ): ScopeResult {
        return if (
            overloads.isEmpty() ||
            (onlyImplicit && overloads.size == 1 && !overloads[0].modifiers.isImplicit)
        )
            ScopeError.NoFuncOverload(
                kind = kind,
                symName = name,
                templateArgs = templateArgs,
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
                templateArgs = templateArgs,
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
        templateArgs: List<TemplateArg>?,
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
                            overloadedFuncSym = sym,
                            from = from,
                            argTypes = effectiveArgTypes,
                            templateArgs = templateArgs
                        )

                        if (bestOverloads.isEmpty())
                            return ScopeError.NoFuncOverload(
                                symName = name,
                                kind = sym.kind,
                                argTypes = argTypes,
                                templateArgs = templateArgs,
                                scopeName = scope.absoluteScopePath,
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
                    templateArgs = templateArgs,
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
        templateArgs: List<TemplateArg>?,
        onlyImplicit: Boolean = false
    ): ScopeResult {
        val costOverloads = resolveBestOverloads(
            overloadedFuncSym = overloadedFunc,
            from = from,
            argTypes = argTypes,
            templateArgs = templateArgs,
            onlyImplicit = onlyImplicit
        )

        return pickSingleFuncSym(
            name = overloadedFunc.name,
            from = from,
            argTypes = argTypes,
            templateArgs = templateArgs,
            kind = overloadedFunc.kind,
            overloads = costOverloads,
            onlyImplicit = onlyImplicit,
        )
    }

    fun resolveOperFunc(
        operator: OperatorType,
        from: Scope,
        argTypes: List<Type>,
        isStatic: Boolean,
        templateArgs: List<TemplateArg>?,
    ): ScopeResult =
        resolveFunc(
            name = operator.fullName,
            kind = FuncKind.OPERATOR,
            from = from,
            argTypes = argTypes,
            isStatic = isStatic,
            templateArgs = templateArgs
        )

    fun resolveConstructor(
        argTypes: List<Type>,
        from: Scope,
        templateArgs: List<TemplateArg>?,
        onlyImplicit: Boolean = false
    ): ScopeResult {
        val overloadedFunc = (scope.symbols.values.find { sym ->
            sym is OverloadedFuncSymbol && sym.kind == FuncKind.CONSTRUCTOR
        } as? OverloadedFuncSymbol)
            ?: return ScopeError.NoFuncOverload(
                symName = Terms.CONSTRUCTOR,
                kind = FuncKind.CONSTRUCTOR,
                argTypes = argTypes,
                templateArgs = templateArgs,
                scopeName = scope.absoluteScopePath,
            ).err()

        return resolveFunc(
            overloadedFunc = overloadedFunc,
            from = from,
            argTypes = argTypes,
            onlyImplicit = onlyImplicit,
            templateArgs = templateArgs
        )
    }
}