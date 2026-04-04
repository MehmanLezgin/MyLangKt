package lang.semantics.scopes

open class LambdaScope(
    override val parent: Scope?
) : Scope(
    parent = parent
)