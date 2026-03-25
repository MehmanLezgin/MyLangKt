package lang.semantics.scopes

import lang.core.operators.OperatorType
import lang.nodes.*
import lang.parser.ParserUtils.isBinOperator
import lang.parser.ParserUtils.isUnaryOperator
import lang.semantics.symbols.*
import lang.semantics.types.ErrorType.castCost
import lang.semantics.types.Type

typealias SymbolMap = MutableMap<String, Symbol>
typealias SymbolIMap = Map<String, Symbol>

open class Scope(
    open val parent: Scope?,
    open val scopeName: String? = null
) {
    val absoluteScopePath: String? by lazy {
        scopeName
            ?.takeIf { it.isNotEmpty() }
            ?.let { name ->
                parent?.absoluteScopePath?.let { "$it::$name" } ?: name
            }
//            ?: parent?.absoluteScopePath
    }

    open val symbols: SymbolMap = mutableMapOf()

    fun Symbol.ok() = ScopeResult.Success(sym = this)
    fun ScopeError.err() = ScopeResult.Error(error = this)

    fun isTypeScope() = this is BaseTypeScope && this !is ModuleScope


    private fun <T : Symbol> defineRaw(sym: T): ScopeResult {
        val name = sym.name
        val definedSym = symbols[name]

        if (definedSym != null)
            return ScopeError.AlreadyDefined(
                symName = name,
                scopeName = absoluteScopePath
            ).err()
        else
            symbols[name] = sym

        return sym.ok()
    }

    fun <T : Symbol> define(sym: T): ScopeResult {
        return when (sym) {
            is FuncSymbol -> defineFunc(sym)
            is OverloadedFuncSymbol -> defineFuncOverload(sym)
            else -> defineRaw(sym)
        }
    }

    private fun defineIfNotExist(sym: Symbol): ScopeResult {
        val existing = symbols[sym.name]
        if (existing != null && existing.javaClass == sym.javaClass)
            return existing.ok()

        return defineRaw(sym)
    }

    open fun isSymVisibleFrom(sym: Symbol, scope: Scope): Boolean {
        return sym.modifiers.visibility == Visibility.PUBLIC
    }

    internal fun resolveRaw(name: String): Symbol? {
        var sym = symbols[name]

        if (sym is AliasSymbol)
            sym = sym.sym

        return sym
    }

    internal fun prepareResult(sym: Symbol, from: Scope): ScopeResult {
        return when {
            !isSymVisibleFrom(sym = sym, scope = from) ->
                ScopeError.Inaccessible(
                    symName = sym.name
                ).err()

            sym is VarSymbol && sym.constValue != null ->
                sym.toConstValueSymbol().ok()

            else -> sym.ok()
        }
    }

    fun resolve(name: String, from: Scope = this, asMember: Boolean = false): ScopeResult {
        if (asMember && this is BaseTypeScope)
            return this.resolveMember(name, from)

        var sym = resolveRaw(name)

        if (sym == null) {
            if (parent == null)
                return ScopeError.NotDefined(
                    symName = name,
                    scopeName = absoluteScopePath
                ).err()

            when (val parentResult = parent!!.resolve(name)) {
                is ScopeResult.ResultList,
                is ScopeResult.Error -> {
                    val enclosingTypeScope = getEnclosingScope<BaseTypeScope>()
                    return enclosingTypeScope?.resolveMember(name, from = enclosingTypeScope)
                        ?: parentResult
                }

                is ScopeResult.Success<*> ->
                    sym = parentResult.sym
            }
        }

        return prepareResult(sym = sym, from = from)
    }

    private fun checkArgumentTypes(
        sym: OverloadedFuncSymbol,
        argTypes: List<Type>,
        condition: (argType: Type, paramType: Type) -> Boolean
    ): FuncSymbol? {
        return sym.overloads.find { funcSym ->
            if (funcSym.params.list.size != argTypes.size)
                return@find false

            val targetParamTypes = funcSym.params.list.map { it.type }

            for (i in targetParamTypes.indices) {
                val argType = argTypes[i]
                val paramType = targetParamTypes[i]

                if (!condition(argType, paramType))
                    return@find false
            }

            return@find true
        }
    }

    fun resolveBestOverloads(
        overloads: List<FuncSymbol>,
        types: List<Type>,
        returnType: Type? = null
    ): List<FuncSymbol> {
        // compute (func, cost) for all valid overloads
        val costs = overloads.mapNotNull { func ->
            if (func.params.list.size != types.size) return@mapNotNull null

            val params = func.params.list
            var totalCost = 0

            for (i in types.indices) {
                val cost = types[i].castCost(params[i].type) ?: return@mapNotNull null
                totalCost += cost
            }

            if (returnType != null) {
                val retCost = func.returnType.castCost(returnType) ?: return@mapNotNull null
                totalCost += retCost
            }

            func to totalCost
        }

        val minCost = costs.minOfOrNull { it.second } ?: return emptyList()

        return costs.filter { it.second == minCost }.map { it.first }
    }

    fun resolveExactOverload(
        overloads: List<FuncSymbol>,
        types: List<Type>,
        returnType: Type? = null
    ): FuncSymbol? {
        return overloads
            .find { func ->
                if (func.params.list.size != types.size)
                    return@find false

                val params = func.params.list

                for (i in types.indices)
                    if (types[i] != params[i].type)
                        return@find false

                if (returnType != null && func.returnType != returnType)
                    return@find false

                true
            }
    }

    fun resolveFunc(
        name: String,
        argTypes: List<Type>,
        isStatic: Boolean = false
    ): ScopeResult {
        return when (val result = resolve(name)) {
            is ScopeResult.ResultList,
            is ScopeResult.Error -> result

            is ScopeResult.Success<*> -> {
                val overloads = when (val sym = result.sym) {
                    is FuncSymbol -> listOf(sym)
                    is OverloadedFuncSymbol -> {
                        val effectiveArgTypes = if (isStatic) argTypes else argTypes.drop(1)

                        val bestOverloads = resolveBestOverloads(sym.overloads, effectiveArgTypes)

                        if (bestOverloads.isEmpty())
                            return ScopeError.NoFuncOverload(
                                symName = name,
                                isOperator = sym.isOperator,
                                argTypes = argTypes,
                                scopeName = absoluteScopePath
                            ).err()

                        bestOverloads
                    }

                    else -> return ScopeError.NotDefined(
                        symName = name,
                        scopeName = absoluteScopePath
                    ).err()
                }

                if (overloads.size > 1)
                    return ScopeError.AmbiguousOverloadedFunc.err()

                val funcSym = overloads[0]

                funcSym.ok()
            }
        }
    }

    fun resolveOperFunc(operator: OperatorType, argTypes: List<Type>, isStatic: Boolean): ScopeResult =
        resolveFunc(operator.fullName, argTypes, isStatic)

    fun defineFunc(
        node: FuncDeclStmtNode,
        nameId: IdentifierNode,
        params: FuncParamListSymbol,
        returnType: Type,
        modifiers: Modifiers
    ): ScopeResult {
        val name = nameId.value

        val funcSym = if (nameId is OperNode) OperatorFuncSymbol(
            operator = nameId.operatorType,
            params = params,
            returnType = returnType,
            modifiers = modifiers
        )
        else {
            when (node) {
                is ConstructorDeclStmtNode -> ConstructorSymbol(
                    name = name,
                    params = params,
                    returnType = returnType,
                    modifiers = modifiers
                )

                is DestructorDeclStmtNode -> DestructorSymbol(
                    name = name,
                    returnType = returnType
                )

                else -> FuncSymbol(
                    name = name,
                    params = params,
                    returnType = returnType,
                    modifiers = modifiers
                )
            }
        }

        return defineFunc(funcSym)
    }

    private fun checkOperatorFunc(funcSym: FuncSymbol): ScopeError? {
        if (funcSym !is OperatorFuncSymbol) return null
        val oper = funcSym.operator

        val isUnary = oper.isUnaryOperator()
        val isBin = oper.isBinOperator()

        val paramsCount = funcSym.params.list.size

        val isStatic = funcSym.modifiers.isStatic
        val factor = if (isStatic) 1 else 0

        val unaryParamCount = 0 + factor
        val binParamCount = 1 + factor
        val isValid = isUnary && paramsCount == unaryParamCount || isBin && paramsCount == binParamCount

        if (isValid) return null

        val paramsExpected = when {
            isUnary && isBin -> 1
            isBin -> 1
            else -> 0
        } + factor


        if (paramsCount != paramsExpected) {
            return ScopeError.OperParamCountMismatch(
                oper = oper,
                expected = paramsExpected,
                isStatic = isStatic
            )
        }

        return null
    }

    private fun defineFuncOverload(funcSym: OverloadedFuncSymbol): ScopeResult {
        return when (val definedSymResult = resolve(funcSym.name)) {
            is ScopeResult.Success<*> -> {
                val results = funcSym.overloads.map { overload ->
                    defineOrOverloadFunction(funcSym = overload, existingSym = definedSymResult.sym)
                }

                ScopeResult.ResultList(list = results)
            }

            is ScopeResult.ResultList,
            is ScopeResult.Error -> defineRaw(sym = funcSym)
        }
    }

    private fun defineFunc(funcSym: FuncSymbol): ScopeResult {
        checkOperatorFunc(funcSym)?.let {
            return it.err()
        }

        return when (val definedSymResult = resolve(funcSym.name)) {
            is ScopeResult.Success<*> ->
                defineOrOverloadFunction(funcSym = funcSym, existingSym = definedSymResult.sym)

            is ScopeResult.ResultList,
            is ScopeResult.Error ->
                defineRaw(sym = funcSym.toOverloadedFuncSymbol()) // always store as overloaded
        }
    }

    private fun defineOrOverloadFunction(funcSym: FuncSymbol, existingSym: Symbol): ScopeResult {
        when (existingSym) {
            is FuncSymbol -> {
                if (existingSym.params == funcSym.params)
                    return ScopeError.ConflictingOverloads.err()

                val overloadedFunc = existingSym.toOverloadedFuncSymbol().apply {
                    overloads.add(funcSym)
                }

                symbols[funcSym.name] = overloadedFunc
                return funcSym.ok()
            }

            is OverloadedFuncSymbol -> {
                if (existingSym.hasOverload(funcSym)) {
                    return ScopeError.AlreadyDefined(funcSym.name, scopeName).err()
                }

                // add anyway, for error: multiple declarations (on call)
                existingSym.overloads.add(funcSym)
                return funcSym.ok()
            }

            else -> return ScopeError.AlreadyDefined(
                symName = funcSym.name,
                scopeName = absoluteScopePath
            ).err()
        }
    }

    fun defineInterface(
        node: InterfaceDeclStmtNode,
        modifiers: Modifiers,
    ): ScopeResult {
        val sym = InterfaceSymbol(
            name = node.name.value,
            scope = InterfaceScope(
                parent = this,
                scopeName = node.name.value,
            ),
            modifiers = modifiers
        )
        return defineRaw(sym)
    }

    fun defineClass(
        node: ClassDeclStmtNode,
        modifiers: Modifiers
    ): ScopeResult {
        val classScope = ClassScope(
            parent = this,
            scopeName = node.name.value
        )

        val sym = ClassSymbol(
            name = node.name.value,
            scope = classScope,
            modifiers = modifiers
        )

        classScope.classSym = sym

        return defineRaw(sym)
    }

    fun defineEnum(node: EnumDeclStmtNode, modifiers: Modifiers): ScopeResult {
        val sym = EnumSymbol(
            name = node.name.value,
            scope = EnumScope(parent = this, scopeName = node.name.value),
            modifiers = modifiers
        )

        return defineRaw(sym)
    }

    fun defineAlias(name: String, sym: Symbol?, visibility: Visibility): ScopeResult {
        val existing = symbols[name]

        if (existing is AliasSymbol && existing.sym == null) {
            existing.sym = sym
            return existing.ok()
        }

        val sym = AliasSymbol(
            name = name,
            sym = sym,
            visibility = visibility
        )

        return defineIfNotExist(sym)
    }

    fun defineFuncNameIfNotExist(name: String, isOperator: Boolean): ScopeResult {
        val sym = OverloadedFuncSymbol(
            name = name,
            isOperator = isOperator,
            overloads = mutableListOf()
        )

        return defineIfNotExist(sym)
    }

    fun defineVarName(
        name: String,
        isMutable: Boolean,
        modifiers: Modifiers
    ): ScopeResult {
        val sym = VarSymbol(
            name = name,
            isMutable = isMutable,
            modifiers = modifiers
        )

        return defineRaw(sym)
    }

    inline fun <reified T : Scope> getEnclosingScope(): T? {
        var curr = this

        while (true) {
            if (curr is T) return curr
            curr = curr.parent ?: return null
        }
    }
}