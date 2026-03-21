package lang.semantics

import lang.messages.MsgHandler
import lang.semantics.symbols.ClassSymbol
import lang.semantics.symbols.VarSymbol
import lang.semantics.types.PrimitiveType
import lang.semantics.types.Type
import lang.semantics.types.UserType
import kotlin.math.max

class TypeLayoutProvider(
    private val msgHandler: MsgHandler
) {
    private val cache = mutableMapOf<Type, TypeLayout>()
    private val inProgress = mutableSetOf<Type>()

    fun getLayout(type: Type): TypeLayout? {
        cache[type]?.let { return it }

        if (!inProgress.add(type))
            msgHandler.semanticError("Illegal recursive type: $type")

        val layout = when (type) {
            is PrimitiveType -> {
                val size = type.primitiveSize.size

                PrimitiveLayout(
                    size = size,
                    align = size
                )
            }

            is UserType -> {
                when (val decl = type.declaration) {
                    is ClassSymbol -> layoutClass(decl)
                    else -> null
                }
            }

            else -> null
        }

        inProgress.remove(type)
        cache[type] = layout ?: return null
        return layout
    }

    private fun layoutClass(cls: ClassSymbol): StructLayout {
        var offset = 0
        var maxAlign = 1
        val fieldsLayout = mutableListOf<FieldLayout>()
        var parentLayouts: List<TypeLayout>? = null

        // ⚠ Важно: порядок должен быть объявленным, не через Map.values
        val fields = cls.scope.instanceScope
            .symbols
            .values
            .filterIsInstance<VarSymbol>()

        cls.superType?.let { superType ->
            val superLayout = getLayout(superType) ?: return@let

            parentLayouts = listOf(superLayout)
//                FieldLayout(
//                    field = superType.declaration!!,
//                    offset = offset,
//                    size = superLayout.size,
//                    align = superLayout.align
//                )
//            )
            val tailPadding = if (superLayout is StructLayout) superLayout.tailPadding else 0

            offset += superLayout.size - tailPadding
            maxAlign = maxOf(maxAlign, superLayout.align)
        }

        for (field in fields) {

            val fieldLayout = getLayout(field.type)
                ?: error("Type ${field.type} has no layout")

            val fieldAlign = fieldLayout.align
            val fieldSize = fieldLayout.size

            // 🔹 выравниваем offset под поле
            offset = alignTo(offset, fieldAlign)

            fieldsLayout += FieldLayout(
                field = field,
                offset = offset,
                size = fieldSize,
                align = fieldAlign,
                isSuper = true
            )

            offset += fieldSize
            maxAlign = maxOf(maxAlign, fieldAlign)
        }

        // 🔹 финальное выравнивание размера структуры
        val finalSize = alignTo(offset, maxAlign)
        val tailPadding = finalSize - offset

        return StructLayout(
            size = max(finalSize, 1),
            align = maxAlign,
            fields = fieldsLayout,
            tailPadding = tailPadding,
            parents = parentLayouts
        )
    }

    private fun alignTo(offset: Int, align: Int): Int =
        ((offset + align - 1) / align) * align

}