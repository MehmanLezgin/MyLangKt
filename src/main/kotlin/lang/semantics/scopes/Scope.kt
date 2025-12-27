package lang.semantics.scopes

import lang.nodes.FuncDeclStmtNode
import lang.nodes.VarDeclStmtNode
import lang.semantics.symbols.FuncDefinition
import lang.semantics.symbols.FuncParamSymbol
import lang.semantics.symbols.FuncSymbol
import lang.semantics.symbols.IncompleteSymbol
import lang.semantics.symbols.Symbol
import lang.semantics.symbols.VarSymbol

open class Scope(
    open val parent: Scope?,
) {
//    private var symTable = SymTable()


/*
    fun resolve(name: String) = symTable.resolve(name = name)

    fun define(sym: Symbol) : Symbol? = symTable.define(sym = sym)*/
internal val symbols = mutableMapOf<String, Symbol>()

    fun define(sym: Symbol) : Symbol? {
        val name = sym.name
        val definedSym = resolve(name)
        if (definedSym != null) {
            when (definedSym) {
                is FuncSymbol -> {
                    if (sym !is FuncSymbol) return null

                    definedSym.definitions.addAll(sym.definitions)
                }
                else -> return null
            }
        }

        symbols[name] = sym
        return sym
    }

    private fun isDefined(name: String): Boolean =
        resolve(name) != null

    private fun isIncomplete(name: String): Boolean =
        resolve(name) is IncompleteSymbol



    fun resolve(name: String): Symbol? =
        symbols[name] ?: parent?.resolve(name)

    fun defineVar(node: VarDeclStmtNode) : Symbol? {
        val sym = VarSymbol(
            name = node.name.value,
            isMutable = node.isMutable
        )

        return define(sym)
    }

    fun defineFunc(node: FuncDeclStmtNode) : Symbol? {
        val params = node.params.map { decl ->
            FuncParamSymbol(
                name = decl.name.value,
                datatype = decl.dataType,
                defaultValue = decl.initializer
            )
        }

        val sym = FuncSymbol.one(
            name = node.name.value,
            definition = FuncDefinition(
                typeNames = node.typeNames,
                params = params,
                returnType = node.returnType
            )
        )

        return define(sym)
    }
}