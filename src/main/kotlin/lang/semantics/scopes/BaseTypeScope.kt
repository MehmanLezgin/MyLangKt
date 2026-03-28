package lang.semantics.scopes

import lang.semantics.symbols.Symbol
import lang.semantics.symbols.Visibility

open class BaseTypeScope(
    override val parent: Scope?,
    override val scopeName: String,
) : Scope(
    parent = parent,
    scopeName = scopeName
) {
    var superTypeScope: BaseTypeScope? = null

    val instanceScope by lazy {
        InstanceScope(
            parent = this,
            scopeName = null
        )
    }

    fun hasSuperTypeScope(scope: BaseTypeScope): Boolean {
        if (superTypeScope == null) return false
        if (superTypeScope == scope) return true
        return superTypeScope!!.hasSuperTypeScope(scope)
    }

    fun resolveMember(
        name: String,
        from: Scope = this,
        scopeName: String? = absoluteScopePath
    ): ScopeResult {
        val sym = resolveRaw(name)

        if (sym != null)
            return prepareResult(sym = sym, from = from, asMember = true)

        val superTypeScope = superTypeScope
            ?: return ScopeError.NotDefined(
                symName = name,
                scopeName = scopeName
            ).err()

        return superTypeScope.resolveMember(name, from)
    }
}