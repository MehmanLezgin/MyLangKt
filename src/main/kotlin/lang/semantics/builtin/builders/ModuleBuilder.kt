package lang.semantics.builtin.builders

import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.ClassScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.ClassSymbol

class ModuleBuilder(
    override val name: String,
    override val parent: Scope
) : BaseTypeBuilder<ClassSymbol>(name, parent) {
    override var typeScope: BaseTypeScope = ClassScope(
        parent = parent,
        scopeName = name,
    )

    override fun build(): ClassSymbol {
        val clazz = ClassSymbol(
            name = name,
            modifiers = modifiers,
            scope = typeScope as ClassScope
        )

        return clazz
    }
}

