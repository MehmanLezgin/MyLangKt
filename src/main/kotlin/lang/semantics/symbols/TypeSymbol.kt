package lang.semantics.symbols

import lang.compiler.Module
import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.ClassScope
import lang.semantics.scopes.EnumScope
import lang.semantics.scopes.InterfaceScope
import lang.semantics.scopes.ModuleExportScope
import lang.semantics.scopes.ModuleScope
import lang.semantics.scopes.NamespaceScope
import lang.semantics.types.PrimitiveType
import lang.semantics.types.Type

open class TypeSymbol(
    override val name: String,
    val staticScope: BaseTypeScope,
    open val superType: Type? = null,
    override val modifiers: Modifiers// = Modifiers()
) : Symbol(name = name, modifiers = modifiers) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypeSymbol

        if (name != other.name) return false
        if (staticScope != other.staticScope) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + staticScope.hashCode()
        return result
    }
}

data class PrimitiveTypeSymbol(
    val type: PrimitiveType,
    val scope: BaseTypeScope,
    override val modifiers: Modifiers = Modifiers()
) : TypeSymbol(
    name = type.name,
    staticScope = scope,
    modifiers = modifiers
)

data class InterfaceSymbol(
    override val name: String,
    val scope: InterfaceScope,
    override val modifiers: Modifiers = Modifiers()
) : TypeSymbol(
    name = name,
    staticScope = scope,
    modifiers = modifiers
)

data class ClassSymbol(
    override val name: String,
    val scope: ClassScope,
    override val modifiers: Modifiers = Modifiers()
) : TypeSymbol(
    name = name,
    staticScope = scope,
    modifiers = modifiers
)

data class EnumSymbol(
    override val name: String,
    val scope: EnumScope,
    override val modifiers: Modifiers = Modifiers()
) : TypeSymbol(
    name = name,
    staticScope = scope,
    modifiers = modifiers
)

open class NamespaceSymbol(
    override val name: String,
    open val scope: NamespaceScope
) : TypeSymbol(
    name = name,
    staticScope = scope,
    modifiers = Modifiers()
)

data class ModuleSymbol(
    override val name: String,
    override val scope: ModuleExportScope
) : NamespaceSymbol(
    name = name,
    scope = scope
)

data class TypedefSymbol(
    override val name: String,
    val type: Type
) : Symbol(name)