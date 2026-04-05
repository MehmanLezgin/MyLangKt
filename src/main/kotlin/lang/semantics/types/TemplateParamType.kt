package lang.semantics.types

import lang.semantics.symbols.TypeSymbol

data class TemplateParamType(
    val param: TemplateParam,
    override var flags: TypeFlags = TypeFlags(),
) : Type() {
    override var declaration: TypeSymbol? = null

    override fun copyWithFlags(flags: TypeFlags) =
        copy(flags = flags)

    override fun toString() = param.name
}
