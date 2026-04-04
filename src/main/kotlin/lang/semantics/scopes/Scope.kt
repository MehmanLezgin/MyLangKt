package lang.semantics.scopes

import lang.infrastructure.LangSpec
import lang.nodes.*
import lang.parser.ParserUtils.isBinOperator
import lang.parser.ParserUtils.isUnaryOperator
import lang.semantics.symbols.*
import lang.semantics.types.Type

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

    open val symbols: MutableMap<String, Symbol> = mutableMapOf()

    fun Symbol.ok() = ScopeResult.Success(sym = this)
    fun ScopeError.err() = ScopeResult.Error(error = this)
    fun List<ScopeResult>.asResultList() = ScopeResult.ResultList(list = this)

    fun isTypeScope() = this is BaseTypeScope && this !is ModuleScope


    private fun <T : Symbol> defineRaw(sym: T): ScopeResult {
        val name = sym.name
        if (LangSpec.isReservedName(name = name))
            return ScopeError.ExpectedName.err()

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

    fun isSymAccessibleFrom(sym: Symbol, from: Scope, asMember: Boolean = false): Boolean {
        val visibility = sym.modifiers.visibility
        if (visibility == Visibility.PUBLIC) return true

        val fromEnclosing = from.getEnclosingScope<BaseTypeScope>()
        val thisEnclosing = this.getEnclosingScope<BaseTypeScope>()

        if (fromEnclosing == thisEnclosing) return true

        return when (visibility) {
            Visibility.PRIVATE -> !asMember && thisEnclosing == fromEnclosing
            Visibility.INTERNAL -> {
                if (thisEnclosing == null) return false
                if (!asMember && thisEnclosing == fromEnclosing) return true
                fromEnclosing?.hasSuperTypeScope(thisEnclosing) == true
            }
        }
    }

    internal fun resolveRaw(name: String): Symbol? {
        var sym = symbols[name]

        if (sym is AliasSymbol)
            sym = sym.sym

        return sym
    }

    internal fun prepareResult(sym: Symbol, from: Scope, asMember: Boolean): ScopeResult {
        return when {
            !isSymAccessibleFrom(sym = sym, from = from, asMember = asMember) ->
                ScopeError.Inaccessible(
                    symName = sym.name
                ).err()

            sym is VarSymbol && sym.constValue != null ->
                sym.toConstValueSymbol().ok()

            else -> sym.ok()
        }
    }

    fun resolve(name: String, from: Scope = this, asMember: Boolean = false): ScopeResult {
        if (asMember)
            when (this) {
                is BaseTypeScope -> return this.resolveMember(name, from)
                is InstanceScope -> return this.resolveMember(name, from)
            }

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

        return prepareResult(
            sym = sym,
            from = from,
            asMember = asMember
        )
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
            initialReturnType = returnType,
            modifiers = modifiers
        )
        else {
            when (node) {
                is ConstructorDeclStmtNode -> ConstructorSymbol(
                    params = params,
                    returnType = returnType,
                    modifiers = modifiers
                )

                is DestructorDeclStmtNode -> DestructorSymbol(
                    returnType = returnType
                )

                else -> when {
                    this is InstanceScope -> {
                        MethodFuncSymbol(
                            accessScope = this,
                            name = name,
                            params = params,
                            initialReturnType = returnType,
                            modifiers = modifiers
                        )
                    }

                    else -> FuncSymbol(
                        name = name,
                        params = params,
                        initialReturnType = returnType,
                        modifiers = modifiers
                    )
                }
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
                funcSym.overloads.map { overload ->
                    defineOrOverloadFunction(funcSym = overload, existingSym = definedSymResult.sym)
                }.asResultList()
            }

            is ScopeResult.ResultList,
            is ScopeResult.Error -> defineRaw(sym = funcSym)
        }
    }

    private fun defineFunc(funcSym: FuncSymbol): ScopeResult {
        checkOperatorFunc(funcSym)?.let {
            return it.err()
        }

        return when (val definedSymResult = resolve(name = funcSym.name, from = this, asMember = true)) {
            is ScopeResult.Success<*> ->
                defineOrOverloadFunction(funcSym = funcSym, existingSym = definedSymResult.sym)

            is ScopeResult.ResultList,
            is ScopeResult.Error ->
                defineRaw(sym = funcSym.toOverloadedFuncSymbol(accessScope = this)) // always store as overloaded
        }
    }

    private fun defineOrOverloadFunction(funcSym: FuncSymbol, existingSym: Symbol): ScopeResult {
        when (existingSym) {
            is FuncSymbol -> {
                if (existingSym.params == funcSym.params)
                    return ScopeError.ConflictingOverloads.err()

                val overloadedFunc = existingSym.toOverloadedFuncSymbol(accessScope = this).apply {
                    overloads.add(funcSym)
                }

                symbols[funcSym.name] = overloadedFunc
                return funcSym.ok()
            }

            is OverloadedFuncSymbol -> {
                if (existingSym.hasOverload(funcSym)) {
                    return ScopeError.AlreadyDefined(funcSym.name, scopeName).err()
                }

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
        val interfaceScope = InterfaceScope(
            parent = this,
            scopeName = node.name.value,
        )

        val sym = InterfaceSymbol(
            name = node.name.value,
            scope = interfaceScope,
            modifiers = modifiers
        )

        interfaceScope.ownerSymbol = sym

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

        classScope.ownerSymbol = sym

        return defineRaw(sym)
    }

    fun defineEnum(node: EnumDeclStmtNode, modifiers: Modifiers): ScopeResult {
        val enumScope = EnumScope(
            parent = this,
            scopeName = node.name.value
        )

        val sym = EnumSymbol(
            name = node.name.value,
            scope = enumScope,
            modifiers = modifiers
        )

        enumScope.ownerSymbol = sym

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

    fun defineFuncNameIfNotExist(
        name: String,
        kind: FuncKind
    ): ScopeResult {
        val sym = when {
            this is InstanceScope ->
                OverloadedMethodSymbol(
                    name = name,
                    kind = kind,
                    overloads = mutableListOf(),
                    accessScope = this
                )

            else ->
                OverloadedFuncSymbol(
                    name = name,
                    kind = kind,
                    overloads = mutableListOf(),
                    accessScope = this
                )
        }

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