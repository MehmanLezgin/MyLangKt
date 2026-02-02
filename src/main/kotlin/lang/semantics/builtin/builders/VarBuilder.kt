package lang.semantics.builtin.builders

import lang.semantics.symbols.ConstVarSymbol
import lang.semantics.symbols.Modifiers
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

    fun build() = ConstVarSymbol(
        name = name,
        type = type,
        value = value,
        modifiers = modifiers
    )
}