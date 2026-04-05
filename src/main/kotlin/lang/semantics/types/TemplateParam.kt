package lang.semantics.types

sealed class TemplateParam(open val name: String) {
    data class TypeParam(
        override val name: String,
        val bound: Type?,
    ) : TemplateParam(name) {
        override fun toString() = "$name: $bound"
    }

    data class ConstValueParam(
        override val name: String,
        val type: Type
    ) : TemplateParam(name) {
        override fun toString() = "$name: $type"
    }
}