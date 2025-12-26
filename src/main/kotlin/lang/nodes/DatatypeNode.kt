package lang.nodes

import lang.tokens.Pos

abstract class BaseDatatypeNode(
    override val pos: Pos
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode =
        mapper(this)
}

data class DatatypeNode(
    val identifier: IdentifierNode,
    val typeNames: List<ExprNode>?,
    val isConst: Boolean = false,
    val ptrLvl: Int = 0,
    var isReference: Boolean = false,
    override val pos: Pos
) : BaseDatatypeNode(pos) {
    override fun toString(): String {
        if (typeNames == null) return identifier.value
        return identifier.value + typeNames
    }

    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            identifier = identifier.mapRecursive(mapper) as? IdentifierNode
                ?: identifier,
            typeNames = typeNames?.map {
                it.mapRecursive(mapper)
            }
        )
        return mapper(newNode)
    }
}

data class FuncDatatypeNode(
    val paramDatatypes: List<BaseDatatypeNode>,
    val returnDatatype: BaseDatatypeNode,
    val isConst: Boolean = false,
    val ptrLvl: Int = 0,
    var isReference: Boolean = false,
    override val pos: Pos
) : BaseDatatypeNode(pos) {
    override fun toString(): String {
        return StringBuilder().apply {
            if (isConst)
                append("const ")

            append("func")

            for (i in 0..ptrLvl)
                append('*')

            append('(')
            if (paramDatatypes.isNotEmpty()) {
                for (i in paramDatatypes.indices) {
                    append(paramDatatypes[i])
                    if (i < paramDatatypes.size - 1)
                        append(", ")
                }
            }
            append(") : ")
            append(returnDatatype)

        }.toString()
    }

    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            paramDatatypes = paramDatatypes.map {
                it.mapRecursive(mapper) as? BaseDatatypeNode ?: it
            },
            returnDatatype = returnDatatype.mapRecursive(mapper) as? BaseDatatypeNode
                ?: returnDatatype
        )
        return mapper(newNode)
    }
}

data class ErrorDatatypeNode(
    override val pos: Pos,
    var isReference: Boolean = false
) : BaseDatatypeNode(pos) {
    override fun toString(): String {
        return "[ ERROR ]"
    }

    override fun mapRecursive(mapper: NodeTransformFunc) =
        mapper(this)
}

data class VoidDatatypeNode(
    override val pos: Pos,
    var isReference: Boolean = false
) : BaseDatatypeNode(pos) {
    companion object {
        const val NAME = "void"
    }

    override fun toString() = NAME

    override fun mapRecursive(mapper: NodeTransformFunc) =
        mapper(this)
}

data class AutoDatatypeNode(
    override val pos: Pos,
    var isReference: Boolean = false
) : BaseDatatypeNode(pos) {
    companion object {
        const val NAME = "auto"
    }

    override fun toString() = NAME

    override fun mapRecursive(mapper: NodeTransformFunc) =
        mapper(this)
}