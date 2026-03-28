package lang.semantics.scopes

import lang.messages.Terms
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.Symbol

class ModuleScope(
    override val parent: Scope?,
    override val scopeName: String,
    sharedSymbols: MutableMap<String, Symbol> = mutableMapOf(),
) : BaseTypeScope(
    parent = parent,
    scopeName = scopeName
) {
    override val symbols = sharedSymbols
    override fun toString(): String {
        return ""
    }

    fun resolveModule(name: String, fromScope: Scope): ScopeResult {
        when (val result = resolve(name = name, from = fromScope, asMember = true)) {
            is ScopeResult.Error,
            is ScopeResult.ResultList -> return result
            is ScopeResult.Success<*> -> {
                if (result.sym is ModuleSymbol)
                    return result
            }
        }

        return ScopeError.NotDefined(
            itemKind = Terms.MODULE,
            symName = name,
            scopeName = absoluteScopePath
        ).err()
    }

    fun defineModules(modules: List<ModuleSymbol>) : ScopeResult.ResultList {
        return modules.map { module ->
            define(sym = module)
        }.asResultList()
    }


}