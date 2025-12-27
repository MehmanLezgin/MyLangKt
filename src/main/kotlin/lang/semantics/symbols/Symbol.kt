package lang.semantics.symbols

import lang.semantics.scopes.Scope

open class Symbol(
    open val name: String
)
data class BuiltInTypeSymbol(
    override val name: String
) : Symbol(name)

data class IncompleteSymbol(
    override val name: String
) : Symbol(name)

data class VarSymbol(
    override val name: String,
    val isMutable: Boolean,
    val isParameter: Boolean = false,
) : Symbol(name)


data class ConstructorSymbol(
    override val name: String,
) : Symbol(name)

data class DestructorSymbol(
    override val name: String,
) : Symbol(name)

data class InterfaceSymbol(
    override val name: String,
    var methods: MutableList<FuncSymbol> = mutableListOf()
) : Symbol(name)

data class ClassSymbol(
    override val name: String,
    val scope: Scope
) : Symbol(name)

data class EnumSymbol(
    override val name: String,
) : Symbol(name)
