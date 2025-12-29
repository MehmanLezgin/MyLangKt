package lang.semantics.scopes

import lang.messages.ErrorHandler
import lang.messages.Messages
import lang.nodes.*
import lang.parser.ParserUtils.isBinOperator
import lang.semantics.ConstResolver
import lang.semantics.symbols.*
import lang.tokens.Pos

open class Scope(
    open val parent: Scope?, open val errorHandler: ErrorHandler
) {
//    private var symTable = SymTable()


    /*
        fun resolve(name: String) = symTable.resolve(name = name)

        fun define(sym: Symbol) : Symbol? = symTable.define(sym = sym)*/
    internal val symbols = mutableMapOf<String, Symbol>()

    fun define(sym: Symbol, pos: Pos?): Symbol? {
        val name = sym.name
        val definedSym = symbols[name]

        if (definedSym != null) {
            semanticError(Messages.SYMBOL_ALREADY_DEFINED.format(name), pos)
        } else
            symbols[name] = sym

        return sym
    }

    private fun paramsEquals(
        params1: List<FuncParamSymbol>, params2: List<FuncParamSymbol>
    ): Boolean {
        if (params1.size != params2.size) return false

        for (i in params1.indices) {
            val param1 = params1[i]
            val param2 = params2[i]
            if (param1 != param2) return false
        }

        return true
    }

    private fun isDefined(name: String): Boolean = resolve(name) != null

    fun resolve(name: String, asMember: Boolean = false): Symbol? =
        if (asMember) symbols[name]
        else symbols[name] ?: parent?.resolve(name)

    fun defineConstVar(node: VarDeclStmtNode): ConstVarSymbol<*> {
        val name = node.name

        val constValue = ConstResolver.resolve(node.initializer, scope = this)

        if (constValue == null)
            semanticError(Messages.EXPECTED_CONST_VALUE, node.pos)

        val sym = ConstVarSymbol(
            name = name.value,
            value = constValue
        )

        node.symbol = sym
        define(sym, name.pos)
        return sym
    }

    fun defineVar(node: VarDeclStmtNode): VarSymbol {
        val name = node.name

        val sym = VarSymbol(
            name = name.value, isMutable = node.isMutable
        )

        node.symbol = sym
        define(sym, name.pos)
        return sym
    }

    fun defineFunc(node: FuncDeclStmtNode, params: FuncParamListSymbol): FuncSymbol {
        val name = node.name
        val sym = if (name is OperNode) OperatorFuncSymbol(
            operator = name.type,
            typeNames = node.typeNames,
            params = params,
            returnType = node.returnType
        )
        else {
            when (node) {
                is ConstructorDeclStmtNode -> ConstructorSymbol(
                    name = name.value,
                    params = params,
                    returnType = node.returnType
                )

                is DestructorDeclStmtNode -> DestructorSymbol(
                    name = name.value,
                    returnType = node.returnType
                )

                else -> FuncSymbol(
                    name = name.value,
                    typeNames = node.typeNames,
                    params = params,
                    returnType = node.returnType
                )
            }
        }

        node.symbol = sym
        return defineFunc(sym, node.name.pos)
    }

    private fun checkOperatorFunc(funcSym: FuncSymbol, pos: Pos) {
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
                0 -> Messages.OPERATOR_REQUIRES_NO_PARAMS
                1 -> Messages.OPERATOR_REQUIRES_1_PARAM
                else -> Messages.OPERATOR_REQUIRES_X_PARAMS
            }.format(prefix, operStr, paramsRequired)

            semanticError(msg, pos)
        }
    }

    private fun defineFunc(funcSym: FuncSymbol, pos: Pos): FuncSymbol {
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
                if (definedSym.hasOverload(funcSym))
                    semanticError(Messages.REDECLARATION, pos)

                // add anyway, for error: multiple declarations (on call)
                definedSym.overloads.add(funcSym)
            }

            else -> semanticError(Messages.SYMBOL_ALREADY_DEFINED.format(name), pos)
        }

        return funcSym
    }

    fun defineInterface(node: InterfaceDeclStmtNode): InterfaceSymbol {
        val sym = InterfaceSymbol(
            name = node.name.value,
            scope = InterfaceScope(parent = this, errorHandler = errorHandler)
        )
        define(sym, node.name.pos)
        return sym
    }

    fun defineClass(node: ClassDeclStmtNode): ClassSymbol {
        val sym = ClassSymbol(
            name = node.name.value,
            scope = ClassScope(parent = this, errorHandler = errorHandler)
        )
        define(sym, node.name.pos)
        return sym
    }

    fun defineEnum(node: EnumDeclStmtNode): EnumSymbol {
        val sym = EnumSymbol(
            name = node.name.value,
            scope = EnumScope(parent = this, errorHandler = errorHandler)
        )

        return sym
    }

    fun getSymbolScope(name: String): Scope? {
        val sym = resolve(name) ?: return null

        when (sym) {

        }
        return null
    }

    fun semanticError(msg: String, pos: Pos?) {
        errorHandler.semanticError(msg, pos)
    }

    fun defineTypedef(node: TypedefStmtNode): TypedefSymbol {
        val sym = TypedefSymbol(
            name = node.identifier.value,
            typename = node.dataType
        )

        define(sym, node.identifier.pos)
        return sym
    }
}