package lang.nodes

import lang.tokens.Pos

typealias NodeTransformFunc = (ExprNode) -> ExprNode

abstract class ExprNode(
    open val pos: Pos
) {
    abstract fun mapRecursive(mapper: NodeTransformFunc): ExprNode
}

object VoidNode : ExprNode(Pos()) {
    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}

data class UnknownNode(override val pos: Pos) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}

data class IdentifierNode(
    val value: String,
    override val pos: Pos
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode =
        mapper(this)
}

data class LiteralNode<T>(
    val value: T,
    override val pos: Pos
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode =
        mapper(this)
}

data class NullLiteralNode(
    override val pos: Pos
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode =
        mapper(this)
}

open class BinOpNode(
    open val left: ExprNode,
    open val right: ExprNode,
    val operator: BinOpType,
    override val pos: Pos
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = BinOpNode(
            left = left.mapRecursive(mapper),
            right = right.mapRecursive(mapper),
            operator = operator,
            pos = pos
        )
        return mapper(newNode)
    }
}

open class UnaryOpNode(
    open val operand: ExprNode,
    val operator: UnaryOpType,
    override val pos: Pos
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = UnaryOpNode(
            operand = operand.mapRecursive(mapper),
            operator = operator,
            pos = pos
        )
        return mapper(newNode)
    }
}

data class FuncCallNode(
    val name: ExprNode,
    val args: List<ExprNode>,
    val typeNames: List<ExprNode>?,
    override val pos: Pos
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            name = name.mapRecursive(mapper),
            args = args.map { it.mapRecursive(mapper) },
            typeNames = typeNames?.map { it.mapRecursive(mapper) }
        )
        return mapper(newNode)
    }
}

data class IndexAccessNode(
    val target: ExprNode,
    val indexExpr: ExprNode,
    override val pos: Pos
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            target = target.mapRecursive(mapper),
            indexExpr = indexExpr.mapRecursive(mapper)
        )
        return mapper(newNode)
    }
}

data class IncrementNode(
    override val operand: ExprNode,
    val isPost: Boolean,
    override val pos: Pos
) : UnaryOpNode(
    operand = operand,
    operator = UnaryOpType.INCREMENT,
    pos = pos
)

data class DecrementNode(
    override val operand: ExprNode,
    val isPost: Boolean,
    override val pos: Pos
) : UnaryOpNode(
    operand = operand,
    operator = UnaryOpType.DECREMENT,
    pos = pos
)

open class BlockNode(
    open val nodes: List<ExprNode>,
    override val pos: Pos
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = BlockNode(
            nodes = nodes.map { it.mapRecursive(mapper) },
            pos = pos
        )
        return mapper(newNode)
    }

    companion object {
        val EMPTY = BlockNode(emptyList(), Pos())
    }
}

data class LambdaNode(
    val body: BlockNode,
    val params: List<VarDeclStmtNode>?,
    override val pos: Pos,
) : ExprNode(pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            body = body.mapRecursive(mapper) as? BlockNode ?: body,
            params = params?.map {
                it.mapRecursive(mapper) as? VarDeclStmtNode ?: it
            }
        )
        return mapper(newNode)
    }
}

data class InitialiserList(
    override val nodes: List<ExprNode>,
    override val pos: Pos
) : BlockNode(nodes, pos) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            nodes = nodes.map { it.mapRecursive(mapper) }
        )
        return mapper(newNode)
    }
}
