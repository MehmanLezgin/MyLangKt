package lang.semantics.builtin.builders

import lang.semantics.symbols.Modifiers
import lang.semantics.symbols.VarSymbol
import lang.semantics.types.ConstValue
import lang.semantics.types.Type

class ConstVarBuilder(
    val name: String,
    val type: Type,
    val value: ConstValue<*>,
) {
    private var modifiers: Modifiers = Modifiers()

    fun static() {
        modifiers.isStatic = true
    }

    fun build() = VarSymbol(
        name = name,
        type = type,
        constValue = value,
        modifiers = modifiers,
        isMutable = false
    )
}