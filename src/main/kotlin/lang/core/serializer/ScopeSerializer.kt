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

            map.forEach { child ->
                when (val value = child.value) {
                    is MutableMap<*, *> -> {
                        value.forEach {
                            map["sym[${it.key}]"] = it.value
                        }
                    }
                    else -> {}
                }
            }

            map.mapWithSymbols("    $currIndent")
        }
    }

    fun getScopeChildren(scope: Scope): ChildrenMapRaw {
        return when (scope) {
            is BaseTypeScope -> mapOf(
                "parent" to scope.parent,
                "scopeName" to scope.scopeName,
                "superTypeScope" to scope.superTypeScope,
                "symbols" to scope.symbols
            )

            is FuncParamsScope -> mapOf(
                "parent" to scope.parent,
                "symbols" to scope.symbols
            )

            is FuncScope -> mapOf(
                "parent" to scope.parent,
                "funcSymbol" to scope.funcSymbol,
                "symbols" to scope.symbols
            )

            else -> emptyMap()
        }
    }
}