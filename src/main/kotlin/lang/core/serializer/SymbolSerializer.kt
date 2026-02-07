package lang.core.serializer

import lang.semantics.symbols.*

object SymbolSerializer {
    fun serialize(
        root: Symbol,
        indent: String = "",
        isLast: Boolean = false,
    ): String {
        return serialize(root, indent, isLast, Symbol::class) { sym, _, _ ->
            getSymbolChildren(sym)
        }
    }

    fun getSymbolChildren(sym: Symbol): ChildrenMapRaw {
        return when (sym) {
            is ConstValueSymbol -> mapOf(
                "type" to sym.type,
                "value" to sym.value
            )

            is ConstVarSymbol -> mapOf(
                "name" to sym.name,
                "type" to sym.type,
                "value" to sym.value
            )

            is FuncParamSymbol -> mapOf(
                "name" to sym.name,
                "type" to sym.type,

                )

            is FuncSymbol -> mapOf(
                "name" to sym.name,
                "params" to sym.params.list,
                "returnType" to sym.returnType,
                "isExtension" to sym.isExtension,
                "modifiers" to sym.modifiers
            )

            is OverloadedFuncSymbol -> mapOf(
                "name" to sym.name,
                "isOperator" to sym.isOperator,
                "overloads" to sym.overloads,
                "modifiers" to sym.modifiers
            )

            is TypeSymbol -> mapOf(
                "name" to sym.name,
                "staticScope" to sym.staticScope,
                "modifiers" to sym.modifiers,
                "superType" to sym.superType
            )

            is TypedefSymbol -> mapOf(
                "name" to sym.name,
                "type" to sym.type,
            )

            is VarSymbol -> mapOf(
                "name" to sym.name,
                "type" to sym.type,
                "isMutable" to sym.isMutable,
                "isParameter" to sym.isParameter,
                "modifiers" to sym.modifiers
            )
        }
    }
}