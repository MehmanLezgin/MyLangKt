package lang.infrastructure.serializer

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

            is FuncParamSymbol -> mapOf(
                "name" to sym.name,
                "type" to sym.type,

                )

            is MethodFuncSymbol -> mapOf(
                "name" to sym.name,
                "[ownerType]" to sym.accessScope.parent.ownerSymbol.type,
                "params" to sym.params.list,
                "returnType" to sym.returnType,
                "isExtension" to sym.isExtension,
                "modifiers" to sym.modifiers
            )

            is FuncSymbol -> mapOf(
                "name" to sym.name,
                "params" to sym.params.list,
                "returnType" to sym.returnType,
                "isExtension" to sym.isExtension,
                "modifiers" to sym.modifiers
            )

            is OverloadedMethodSymbol -> mapOf(
                "name" to sym.name,
                "[ownerType]" to sym.accessScope.parent.ownerSymbol.type,
                "kind" to sym.kind,
                "overloads" to sym.overloads,
                "modifiers" to sym.modifiers
            )

            is OverloadedFuncSymbol -> mapOf(
                "name" to sym.name,
                "kind" to sym.kind,
                "overloads" to sym.overloads,
                "modifiers" to sym.modifiers
            )

            is TypeSymbol -> mapOf(
                "name" to sym.name,
                "staticScope" to sym.staticScope.symbols,
                "modifiers" to sym.modifiers,
                "superType" to sym.superType
            )

            is AliasSymbol -> mapOf(
                "name" to sym.name,
                "sym" to sym.sym,
            )

            is VarSymbol -> mapOf(
                "name" to sym.name,
                "type" to sym.type,
                "constValue" to sym.constValue,
                "isMutable" to sym.isMutable,
                "isParameter" to sym.isParameter,
                "modifiers" to sym.modifiers
            )
        }
    }
}