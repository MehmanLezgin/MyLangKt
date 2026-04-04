package lang.core.builders

import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.ModuleScope
import lang.semantics.symbols.ModuleSymbol

class ModuleBuilder(
    override val name: String
) : BaseTypeBuilder<ModuleSymbol>(name) {

    override var typeScope: BaseTypeScope = ModuleScope(
        parent = parent,
        scopeName = name
    )

    override fun build(): ModuleSymbol {
        val sym = ModuleSymbol(
            name = name,
            modifiers = modifiers,
            scope = typeScope as ModuleScope
        )

        return sym
    }
}

