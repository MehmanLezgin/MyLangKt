package lang.semantics.types

import lang.semantics.symbols.Symbol

class UserType(
    val name: String,
    val templateArgs: List<TemplateArg>,
    val declaration: Symbol,
    override var flags: TypeFlags = TypeFlags()
) : Type(
    flags = flags,
) {
    override fun copyWithFlags(flags: TypeFlags) =
        UserType(
            name = name,
            templateArgs = templateArgs,
            declaration = declaration,
            flags = flags
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as UserType

        if (name != other.name) return false
        if (templateArgs != other.templateArgs) return false
        if (declaration != other.declaration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + templateArgs.hashCode()
        result = 31 * result + declaration.hashCode()
        return result
    }

    override fun toString() = stringify()
}