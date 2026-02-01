package lang.semantics.symbols

import lang.nodes.ExprNode
import lang.semantics.types.ConstValue
import lang.semantics.types.Type

open class Symbol(
    open val name: String,
    open val modifiers: Modifiers = Modifiers()
)

data class VarSymbol(
    override val name: String,
//    val identifier: IdentifierNode,
    val type: Type,
    val isMutable: Boolean,
    val isParameter: Boolean = false,
    override val modifiers: Modifiers = Modifiers()
) : Symbol(name = name, modifiers = modifiers)

data class ConstVarSymbol(
    override val name: String,
    val type: Type,
    val value: ConstValue<*>?,
    override val modifiers: Modifiers = Modifiers()
) : Symbol(name = name, modifiers = modifiers) {
    fun toConstValueSymbol() : ConstValueSymbol? {
        return value?.let { ConstValueSymbol.from(it) }
    }
}

data class ConstValueSymbol(
    val type: Type,
    val value: ConstValue<*>?
) : Symbol(name = "") {
    companion object {
        fun from(constValue: ConstValue<*>): ConstValueSymbol {
            return ConstValueSymbol(
                type = constValue.type,
                value = constValue
            )
        }
    }
}


//fun <T : Symbol> T.attachSymbol(vararg nodes: ExprNode): T {
//    nodes.forEach { node -> node.symbol = this}
//    return this
//}