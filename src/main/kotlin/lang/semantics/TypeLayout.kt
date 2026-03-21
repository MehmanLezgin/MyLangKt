package lang.semantics

import lang.semantics.symbols.Symbol

sealed interface TypeLayout {
    val size: Int
    val align: Int
}

data class PrimitiveLayout(
    override val size: Int,
    override val align: Int
) : TypeLayout

data class StructLayout(
    override val size: Int,
    override val align: Int,
    val parents: List<TypeLayout>? = null,
    val fields: List<FieldLayout>,
    val tailPadding: Int
) : TypeLayout {
    fun getField(fieldName: String): FieldLayout? {
        return fields.find { field -> field.field.name == fieldName }
    }

    fun getFieldWithSuper(fieldName: String): FieldLayout? {
        getField(fieldName)?.let { return it }
        if (parents == null) return null

        for (parent in parents) {
            if (parent !is StructLayout) continue
            parent.getFieldWithSuper(fieldName = fieldName)?.let { return it }
        }

        return null
    }

    override fun toString(): String {
        return buildString {
            append("{")
            fields.forEach { field ->
                append("\n\t${field.field.name} @${field.offset} : size = ${field.size}, align = ${field.align}")
            }
            append("\n\tsize = $size,\n\talign = $align,\n\ttailPadding = $tailPadding")
            append("\n}")
        }
    }
}

class FieldLayout(
    override val size: Int,
    override val align: Int,
    val offset: Int,
    val field: Symbol,
    val isSuper: Boolean = false
) : TypeLayout