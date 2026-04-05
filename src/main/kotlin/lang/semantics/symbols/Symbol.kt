package lang.semantics.symbols

import lang.semantics.types.ConstValue
import lang.semantics.types.Type
import lang.semantics.types.UnresolvedType
import lang.semantics.types.lazyType

sealed interface Symbol {
    val name: String
    val modifiers: Modifiers
}

open class VarSymbol(
    override val name: String,
    open var initialType: Type = UnresolvedType,
    val isMutable: Boolean,
    val isParameter: Boolean = false,
    var constValue: ConstValue<*>? = null,
    override val modifiers: Modifiers = Modifiers()
) : Symbol {
    private var lazyType = lazyType { initialType }

    var type: Type
        get() = lazyType.type
        set(value) {
            lazyType = lazyType { value }
        }

    fun toConstValueSymbol() = ConstValueSymbol(
        type = type,
        value = constValue
    )

    val isConst: Boolean
        get() = constValue != null

}

/*
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
*/

data class ConstValueSymbol(
    val type: Type,
    val value: ConstValue<*>?,
    override val name: String = "",
    override val modifiers: Modifiers = Modifiers()
) : Symbol {
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