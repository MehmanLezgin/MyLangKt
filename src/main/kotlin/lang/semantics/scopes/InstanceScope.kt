package lang.semantics.scopes

class InstanceScope(
    override val parent: BaseTypeScope,
    override val scopeName: String?,
) : Scope(
    parent = parent,
    scopeName = scopeName
) {
    fun resolveMember(
        name: String,
        from: Scope = this,
        scopeName: String? = absoluteScopePath
    ): ScopeResult {
        val sym = resolveRaw(name)

        if (sym != null)
            return prepareResult(sym = sym, from = from, asMember = true)

        val superTypeScope = parent.superTypeScope?.instanceScope
            ?: return ScopeError.NotDefined(
                symName = name,
                scopeName = scopeName
            ).err()

        return superTypeScope.resolveMember(name, from)
    }

}