package lang.semantics.scopes

import lang.nodes.*
import lang.parser.ParserUtils.isBinOperator
import lang.semantics.symbols.*
import lang.semantics.types.ConstValue
import lang.semantics.types.ErrorType.castCost
import lang.semantics.types.Type
import lang.core.operators.OperatorType

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
            ?: parent?.absoluteScopePath
    }


    internal val symbols = mutableMapOf<String, Symbol>()

    fun Symbol.ok() = ScopeResult.Success(sym = this)
    fun ScopeError.err() = ScopeResult.Error(error = this)

    fun <T : Symbol> define(sym: T, visibility: Visibility = Visibility.PUBLIC): ScopeResult {
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

    fun resolve(name: String, asMember: Boolean = false): ScopeResult {
        var sym = symbols[name]

        if (sym is AliasSymbol) sym = sym.sym

        if (sym == null) {
            if (asMember || parent == null)
                return ScopeError.NotDefined(
                    symName = name,
                    scopeName = absoluteScopePath
                ).err()

            when (val parentResult = parent!!.resolve(name)) {
                is ScopeResult.Error -> return parentResult
                is ScopeResult.Success<*> -> sym = parentResult.sym
            }
        }

        if (sym is ConstVarSymbol)
            sym = sym.toConstValueSymbol() ?: return ScopeError.InvalidConstValue.err()

        return sym.ok()
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
        argTypes: List<Type>
    ): ScopeResult {
        return when (val result = resolve(name)) {
            is ScopeResult.Error -> result
            is ScopeResult.Success<*> -> {
                val overloads = when (val sym = result.sym) {
                    is FuncSymbol -> listOf(sym)
                    is OverloadedFuncSymbol -> {
                        resolveBestOverloads(sym.overloads, argTypes).also {
                            if (it.isEmpty())
                                return ScopeError.NoFuncOverload(
                                    symName = name,
                                    isOperator = sym.isOperator,
                                    argTypes = argTypes,
                                    scopeName = absoluteScopePath
                                ).err()
                        }
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

    fun resolveOperFunc(operator: OperatorType, argTypes: List<Type>): ScopeResult =
        resolveFunc(operator.fullName, argTypes)

    fun defineConstVar(
        node: VarDeclStmtNode,
        type: Type,
        constValue: ConstValue<*>?,
        modifiers: Modifiers
    ): ScopeResult {
        val name = node.name

        val sym = ConstVarSymbol(
            name = name.value,
            type = type,
            value = constValue,
            modifiers = modifiers
        )

        return define(sym)
    }

    fun defineVar(node: VarDeclStmtNode, type: Type, modifiers: Modifiers): ScopeResult {
        val name = node.name

        val sym = VarSymbol(
            name = name.value,
            type = type,
            isMutable = node.isMutable,
            modifiers = modifiers
        )

        return define(sym)
    }

    /*fun defineFunc(
        node: FuncDeclStmtNode,
        params: FuncParamListSymbol,
        returnType: Type,
        modifiers: Modifiers
    ): Pair<ScopeResult, FuncSymbol> {
        val name = node.name
        val funcSym = if (name is OperNode) OperatorFuncSymbol(
            operator = name.operatorType,
            params = params,
            returnType = returnType,
            modifiers = modifiers
        )
        else {
            val name = if (node.name is IdentifierNode)
                (node.name as IdentifierNode).value else node.name.toString()

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

        return defineFunc(funcSym) to funcSym
    }*/

    fun defineFunc(
        node: FuncDeclStmtNode,
        nameId: IdentifierNode,
        params: FuncParamListSymbol,
        returnType: Type,
        modifiers: Modifiers
    ): Pair<ScopeResult, FuncSymbol> {
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

        return defineFunc(funcSym) to funcSym
    }

    private fun checkOperatorFunc(funcSym: FuncSymbol): ScopeError? {
        if (funcSym !is OperatorFuncSymbol) return null
        val oper = funcSym.operator

        val paramsExpected = when {
            oper.isBinOperator() -> 2
            else -> 1
        }

//        val isNonStatic = this !is ClassScope && this !is InterfaceScope

//        if (isNonStatic)
//            paramsExpected--

        if (funcSym.params.list.size != paramsExpected) {
            return ScopeError.OperParamCountMismatch(
                oper = oper,
                expected = paramsExpected
            )
        }

        return null
    }

    fun defineFunc(funcSym: FuncSymbol): ScopeResult {
        checkOperatorFunc(funcSym)?.let {
            return it.err()
        }

        return when (val definedSymResult = resolve(funcSym.name)) {
            is ScopeResult.Success<*> ->
                defineOrOverloadFunction(funcSym = funcSym, existingSym = definedSymResult.sym)

            is ScopeResult.Error ->
                define(sym = funcSym.toOverloadedFuncSymbol()) // always store as overloaded
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
                return overloadedFunc.ok()
            }

            is OverloadedFuncSymbol -> {
                if (existingSym.hasOverload(funcSym)) {
                    return ScopeError.Redeclaration.err()
                }

                // add anyway, for error: multiple declarations (on call)
                existingSym.overloads.add(funcSym)
                return existingSym.ok()
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
        superType: Type?
    ): ScopeResult {
        val sym = InterfaceSymbol(
            name = node.name.value,
            scope = InterfaceScope(
                parent = this,
                scopeName = node.name.value,
                superTypeScope = superType?.declaration?.staticScope
            ),
            modifiers = modifiers
        )
        return define(sym)
    }

    fun defineClass(
        node: ClassDeclStmtNode,
        modifiers: Modifiers,
        superType: Type?
    ): ScopeResult {
        val sym = ClassSymbol(
            name = node.name.value,
            scope = ClassScope(
                parent = this,
                scopeName = node.name.value,
                superTypeScope = superType?.declaration?.staticScope
            ),
            modifiers = modifiers
        )

        return define(sym)
    }

    fun defineEnum(node: EnumDeclStmtNode, modifiers: Modifiers): ScopeResult {
        val sym = EnumSymbol(
            name = node.name.value,
            scope = EnumScope(parent = this, scopeName = node.name.value),
            modifiers = modifiers
        )

        return define(sym)
    }

    fun defineUsing(name: String, sym: Symbol, visibility: Visibility): ScopeResult {
        val sym = AliasSymbol(
            name = name,
            sym = sym,
            visibility = visibility
        )

        return define(sym)
    }

    fun defineModuleIfNotExists(node: ModuleStmtNode): ScopeResult {
        val name = node.name.value

        symbols[name]?.let {
            if (it is ModuleSymbol)
                return ScopeResult.Success(it)
        }

        val sym = ModuleSymbol(
            name = name,
            scope = ModuleScope(
                parent = this,
                scopeName = name
            )
        )

        return define(sym)
    }

    fun resolveModule(name: String): ScopeResult {
        symbols[name]?.let {
            if (it is ModuleSymbol)
                return ScopeResult.Success(it)
        }

        return ScopeResult.Error(ScopeError.NotDefined(name, scopeName))
    }
}