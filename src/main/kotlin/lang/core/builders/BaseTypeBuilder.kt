package lang.core.builders

import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.Modifiers
import lang.semantics.symbols.TypeSymbol

abstract class BaseTypeBuilder<T: TypeSymbol>(
    open val name: String,
    open val parent: Scope? = null
) {
    internal var modifiers: Modifiers = Modifiers()

    open lateinit var typeScope: BaseTypeScope

    abstract fun build(): T

    fun modifiers(modifiers: Modifiers) {
        this.modifiers = modifiers
    }

    fun staticScope(block: ScopeBuilder.() -> Unit) {
        ScopeBuilder(typeScope).apply(block).build()
    }

    fun instanceScope(block: ScopeBuilder.() -> Unit) {
        ScopeBuilder(typeScope.instanceScope).apply(block).build()
    }

    fun superScope(block: ScopeBuilder.() -> Unit) {
        val superTypeScope = typeScope.superTypeScope ?: return
        ScopeBuilder(superTypeScope).apply(block).build()
    }
}

