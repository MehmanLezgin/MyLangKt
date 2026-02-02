package lang.semantics.scopes

import lang.messages.ErrorHandler
import lang.messages.Messages
import lang.nodes.*
import lang.parser.ParserUtils.isBinOperator
import lang.semantics.symbols.*
import lang.semantics.types.ConstValue
import lang.semantics.types.ErrorType.castCost
import lang.semantics.types.Type
import lang.tokens.OperatorType
import lang.tokens.Pos

open class Scope(
    open val parent: Scope?,
    open val errorHandler: ErrorHandler,
    open val scopeName: String? = null
) {
    val absoluteScopePath: String? by lazy {
        when {
            parent?.parent == null -> scopeName
            scopeName == null -> parent?.absoluteScopePath
            parent != null -> "${parent?.absoluteScopePath}::$scopeName"
            else -> scopeName
        }
    }

    internal val symbols = mutableMapOf<String, Symbol>()

    fun define(sym: Symbol, pos: Pos?): Symbol {
        val name = sym.name
        val definedSym = symbols[name]

        if (definedSym != null) {
            semanticError(Messages.F_SYMBOL_ALREADY_DEFINED.format(name), pos)
        } else
            symbols[name] = sym

        return sym
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

    fun resolve(name: String, asMember: Boolean = false): Symbol? {
        var sym = if (asMember) symbols[name]
        else symbols[name] ?: parent?.resolve(name)
        if (sym is ConstVarSymbol)
            sym = sym.toConstValueSymbol()
        return sym
    }

    /*fun resolveFirstOverload(
        sym: OverloadedFuncSymbol,
        argTypes: List<Type>
    ): FuncSymbol? {
        checkArgumentTypes(
            sym = sym,
            argTypes = argTypes,
        ) { argType, paramType ->
            argType == paramType
        }?.let { return it }

        return checkArgumentTypes(
            sym = sym,
            argTypes = argTypes,
        ) { argType, paramType ->
            argType.canCastTo(paramType)
        }
    }*/

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
    ): List<FuncSymbol>? {
        val sym = resolve(name) ?: return null
        return when (sym) {
            is FuncSymbol -> listOf(sym)
            is OverloadedFuncSymbol -> resolveBestOverloads(sym.overloads, argTypes)
            else -> null
        }
    }



    fun resolveOperatorFunc(operator: OperatorType, argTypes: List<Type>): List<FuncSymbol>? =
        resolveFunc(operator.fullName, argTypes)

    fun defineConstVar(
        node: VarDeclStmtNode,
        type: Type,
        constValue: ConstValue<*>?,
        modifiers: Modifiers
    ): ConstVarSymbol {
        val name = node.name

        val sym = ConstVarSymbol(
            name = name.value,
            type = type,
            value = constValue,
            modifiers = modifiers
        )

        /*if (constValue != null && node.initializer != null) {
            ConstValueSymbol
                .from(constValue)
                .attachSymbol(node.initializer)
        }*/

        define(sym, name.pos)
        return sym
    }

    fun defineVar(node: VarDeclStmtNode, type: Type, modifiers: Modifiers): VarSymbol {
        val name = node.name

        val sym = VarSymbol(
            name = name.value,
            type = type,
            isMutable = node.isMutable,
            modifiers = modifiers
        )

        define(sym, name.pos)
        return sym
    }

    fun defineFunc(
        node: FuncDeclStmtNode,
        params: FuncParamListSymbol,
        returnType: Type,
        modifiers: Modifiers
    ): FuncSymbol {
        val name = node.name
        val sym = if (name is OperNode) OperatorFuncSymbol(
            operator = name.operatorType,
//            typeNames = node.typeNames,
            params = params,
            returnType = returnType,
            modifiers = modifiers
        )
        else {
            when (node) {
                is ConstructorDeclStmtNode -> ConstructorSymbol(
                    name = name.value,
                    params = params,
                    returnType = returnType,
                    modifiers = modifiers
                )

                is DestructorDeclStmtNode -> DestructorSymbol(
                    name = name.value,
                    returnType = returnType
                )

                else -> FuncSymbol(
                    name = name.value,
//                    typeNames = node.typeNames,
                    params = params,
                    returnType = returnType,
                    modifiers = modifiers
                )
            }
        }

        return defineFunc(sym, node.name.pos)
    }

    private fun checkOperatorFunc(funcSym: FuncSymbol, pos: Pos?) {
        if (funcSym !is OperatorFuncSymbol) return
        val operator = funcSym.operator

        var paramsRequired = when {
            operator.isBinOperator() -> 2
            else -> 1
        }

        val isNonStatic = this is ClassScope || this is InterfaceScope

        if (isNonStatic)
            paramsRequired--

        if (funcSym.params.list.size != paramsRequired) {
            val operStr = operator.name

            val prefix = if (isNonStatic)
                Messages.NON_STATIC else Messages.STATIC

            val msg = when (paramsRequired) {
                0 -> Messages.F_OPERATOR_REQUIRES_NO_PARAMS
                1 -> Messages.F_OPERATOR_REQUIRES_1_PARAM
                else -> Messages.F_OPERATOR_REQUIRES_X_PARAMS
            }.format(prefix, operStr, paramsRequired)

            semanticError(msg, pos)
        }
    }

    fun defineFunc(funcSym: FuncSymbol, pos: Pos?): FuncSymbol {
        val name = funcSym.name

        checkOperatorFunc(funcSym, pos)

        when (val definedSym = resolve(name)) {
            null -> symbols[name] = funcSym

            is FuncSymbol -> {
                if (definedSym.params == funcSym.params)
                    semanticError(Messages.CONFLICTING_OVERLOADS, pos)

                val overloadedFunc = definedSym.toOverloadedFuncSymbol().apply {
                    overloads.add(funcSym)
                }

                symbols[name] = overloadedFunc
            }

            is OverloadedFuncSymbol -> {
                if (definedSym.hasOverload(funcSym)) {
                    val typesStr = funcSym.params.list.map { it.type }.joinToString()
                    val funcStr = "${funcSym.name}($typesStr) : ${funcSym.returnType}"
                    val msg = "${Messages.REDECLARATION}: $funcStr"
                    semanticError(msg, pos)
                }

                // add anyway, for error: multiple declarations (on call)
                definedSym.overloads.add(funcSym)
            }

            else -> semanticError(Messages.F_SYMBOL_ALREADY_DEFINED.format(name), pos)
        }

        return funcSym
    }

    fun defineInterface(
        node: InterfaceDeclStmtNode,
        modifiers: Modifiers,
        superType: Type?
    ): InterfaceSymbol {
        val sym = InterfaceSymbol(
            name = node.name.value,
            scope = InterfaceScope(
                parent = this,
                errorHandler = errorHandler,
                scopeName = node.name.value,
                superTypeScope = superType?.declaration?.staticScope
            ),
            modifiers = modifiers
        )
        define(sym, node.name.pos)
        return sym
    }

    fun defineClass(
        node: ClassDeclStmtNode,
        modifiers: Modifiers,
        superType: Type?
    ): ClassSymbol {
        val sym = ClassSymbol(
            name = node.name.value,
            scope = ClassScope(
                parent = this,
                errorHandler = errorHandler,
                scopeName = node.name.value,
                superTypeScope = superType?.declaration?.staticScope
            ),
            modifiers = modifiers
        )
        define(sym, node.name.pos)
        return sym
    }

    fun defineEnum(node: EnumDeclStmtNode, modifiers: Modifiers): EnumSymbol {
        val sym = EnumSymbol(
            name = node.name.value,
            scope = EnumScope(parent = this, errorHandler = errorHandler, scopeName = node.name.value),
            modifiers = modifiers
        )

        define(sym, node.name.pos)
        return sym
    }

    fun getSymbolScope(name: String): Scope? {
        val sym = resolve(name)

        if (sym !is TypeSymbol)
            return null

        return sym.staticScope
    }

    fun semanticError(msg: String, pos: Pos?) =
        errorHandler.semanticError(msg, pos)

    fun defineTypedef(node: TypedefStmtNode, type: Type): TypedefSymbol {
        val name = node.name

        val sym = TypedefSymbol(
            name = name.value,
            type = type
        )

        define(sym, name.pos)
        return sym
    }
}