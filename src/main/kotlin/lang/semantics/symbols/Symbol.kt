package lang.semantics.symbols

import lang.nodes.BaseDatatypeNode
import lang.semantics.scopes.ClassScope
import lang.semantics.scopes.InterfaceScope
import lang.semantics.scopes.Scope
import lang.semantics.types.ConstValue

open class Symbol(
    open val name: String
)
data class BuiltInTypeSymbol(
    override val name: String,
    val scope: Scope,
) : Symbol(name)
//
//data class IncompleteSymbol(
//    override val name: String
//) : Symbol(name)

data class VarSymbol(
    override val name: String,
    val isMutable: Boolean,
    val isParameter: Boolean = false,
) : Symbol(name)

data class ConstVarSymbol<T: Any>(
    override val name: String,
    val value: ConstValue<T>?,
) : Symbol(name)

data class InterfaceSymbol(
    override val name: String,
    val scope: InterfaceScope,
    var methods: MutableList<OverloadedFuncSymbol> = mutableListOf()
) : Symbol(name)

data class ClassSymbol(
    override val name: String,
    val scope: ClassScope
) : Symbol(name)

data class EnumSymbol(
    override val name: String,
    val scope: Scope
) : Symbol(name)

data class TypedefSymbol(
    override val name: String,
    val typename: BaseDatatypeNode
) : Symbol(name)