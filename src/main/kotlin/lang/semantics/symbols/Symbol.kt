package lang.semantics.symbols

import lang.nodes.ExprNode
import lang.semantics.scopes.ClassScope
import lang.semantics.scopes.EnumScope
import lang.semantics.scopes.InterfaceScope
import lang.semantics.scopes.Scope
import lang.semantics.types.ConstValue
import lang.semantics.types.PrimitiveType
import lang.semantics.types.Type

open class Symbol(
    open val name: String
)


data class PrimitiveTypeSymbol(
    val type: PrimitiveType,
    val scope: Scope,
) : Symbol(name = type.name)
//
//data class IncompleteSymbol(
//    override val name: String
//) : Symbol(name)

data class VarSymbol(
    override val name: String,
    val type: Type,
    val isMutable: Boolean,
    val isParameter: Boolean = false,
) : Symbol(name)

data class ConstVarSymbol(
    override val name: String,
    val type: Type,
    val value: ConstValue<*>?,
) : Symbol(name)

data class ConstValueSymbol(
    val type: Type,
    val value: ConstValue<*>?,
) : Symbol("")


open class UserTypeSymbol(
    override val name: String,
    val baseScope: Scope
) : Symbol(name = name)


data class InterfaceSymbol(
    override val name: String,
    val scope: InterfaceScope
) : UserTypeSymbol(name = name, baseScope = scope)

data class ClassSymbol(
    override val name: String,
    val scope: ClassScope
) : UserTypeSymbol(name = name, baseScope = scope)

data class EnumSymbol(
    override val name: String,
    val scope: EnumScope
) : UserTypeSymbol(name = name, baseScope = scope)

data class TypedefSymbol(
    override val name: String,
    val type: Type
) : Symbol(name)


fun <T: Symbol> T.attachSymbol(node: ExprNode): T {
    node.symbol = this
    return this
}