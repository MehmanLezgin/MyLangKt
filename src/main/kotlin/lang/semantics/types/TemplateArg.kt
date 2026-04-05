package lang.semantics.types

import lang.semantics.symbols.ConstValueSymbol

sealed class TemplateArg {
    data class ArgType(
        val type: Type,
    ) : TemplateArg() {
        override fun toString() = type.toString()
    }

    data class ArgConstValue(
        val value: ConstValue<*>
    ) : TemplateArg() {
        val constValueSymbol = ConstValueSymbol(
            type = value.type,
            value = value
        )

        override fun toString() = value.value.toString()
    }
}