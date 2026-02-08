package lang.core.serializer

import lang.core.serializer.AstSerializer.mapWithSymbols
import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.FuncParamsScope
import lang.semantics.scopes.FuncScope
import lang.semantics.scopes.Scope

object ScopeSerializer {
    fun serialize(
        root: Scope,
        indent: String = "",
        isLast: Boolean = false,
    ): String {
        return serialize(root, indent, isLast, Scope::class) { sym, currIndent, _ ->
            val map = getScopeChildren(sym).toMutableMap()

            map.toMap().forEach { child ->
                when (val value = child.value) {
                    is MutableMap<*, *> -> {
                        value.forEach {
                            map["sym[\"${it.key}\"]"] = it.value
                        }
                    }

                    else -> {}
                }
            }

            map.mapWithSymbols("    $currIndent")
        }
    }

    fun getScopeChildren(scope: Scope): ChildrenMapRaw {
        val defaultField = mapOf(
            "scopeName" to scope.scopeName,
            "symbols" to scope.symbols
        )

        return when (scope) {
            is BaseTypeScope -> defaultField + mapOf(
                "superTypeScope" to scope.superTypeScope,
            )

            is FuncScope -> defaultField + mapOf(
                "funcSymbol" to scope.funcSymbol,
            )

            else -> defaultField
        }
    }
}