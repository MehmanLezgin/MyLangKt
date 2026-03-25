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

    val instanceScope: Scope by lazy {
        Scope(
            parent = this,
            scopeName = null
        )
    }

    override fun isSymVisibleFrom(sym: Symbol, scope: Scope, asMember: Boolean): Boolean {
        if (super.isSymVisibleFrom(sym, scope, asMember)) return true

        val enclosing = scope.getEnclosingScope<BaseTypeScope>()

        return when (sym.modifiers.visibility) {
            Visibility.PUBLIC -> true
            Visibility.PRIVATE -> !asMember && this == enclosing
            Visibility.INTERNAL -> {
                if (!asMember && this == enclosing) return true
                enclosing?.hasSuperTypeScope(this) == true
            }
        }
    }

    private fun hasSuperTypeScope(scope: BaseTypeScope): Boolean {
        if (superTypeScope == null) return false
        if (superTypeScope == scope) return true
        return superTypeScope!!.hasSuperTypeScope(scope)
    }

    fun resolveMember(
        name: String,
        from: Scope = this,
        scopeName: String? = absoluteScopePath
    ): ScopeResult {
        val sym = instanceScope.resolveRaw(name)

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