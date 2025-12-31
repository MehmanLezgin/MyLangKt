package lang.semantics.types

sealed class TemplateArg {
    data class ArgType(
        val type: Type,
    ) : TemplateArg() {
        override fun toString() = type.toString()
    }

    data class ArgConstValue<T : Any>(
        val value: ConstValue<T>
    ) : TemplateArg() {
        override fun toString() = value.value.toString()
    }
}