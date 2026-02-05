package lang.nodes

import lang.tokens.OperatorType
import lang.core.SourceRange

typealias NodeTransformFunc = (ExprNode) -> ExprNode

interface ExprNode {
    val range: SourceRange
    fun mapRecursive(mapper: NodeTransformFunc): ExprNode
}

object VoidNode : ExprNode {
    override val range: SourceRange = SourceRange()

    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}

data class UnknownNode(override val range: SourceRange) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc) = mapper(this)
}

open class IdentifierNode(
    val value: String,
    override val range: SourceRange
) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode =
        mapper(this)
}

data class OperNode(
    val operatorType: OperatorType,
    override val range: SourceRange
) : IdentifierNode(
    value = operatorType.fullName,
    range = range
) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode =
        mapper(this)
}

open class BinOpNode(
    open val left: ExprNode,
    open val right: ExprNode,
    val operator: BinOpType,
    val tokenOperatorType: OperatorType,
    override val range: SourceRange
) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = BinOpNode(
            left = left.mapRecursive(mapper),
            right = right.mapRecursive(mapper),
            operator = operator,
            tokenOperatorType = tokenOperatorType,
            range = range
        )
        return mapper(newNode)
    }
}

data class DotAccessNode(
    val base: ExprNode,
    val member: IdentifierNode,
    override val range: SourceRange
) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = DotAccessNode(
            base = base.mapRecursive(mapper),
            member = member.mapRecursive(mapper) as? IdentifierNode ?: member,
            range = range
        )
        return mapper(newNode)
    }
}

/*data class ScopeAccessNode(
    val base: QualifiedDatatypeNode,
    val member: IdentifierNode,
    override val range: SourceRange
) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = ScopeAccessNode(
            base = base.mapRecursive(mapper) as? QualifiedDatatypeNode ?: base,
            member = member.mapRecursive(mapper) as? IdentifierNode ?: member,
            range = range
        )

        return mapper(newNode)
    }
}*/



open class UnaryOpNode(
    open val operand: ExprNode,
    val operator: UnaryOpType,
    val tokenOperatorType: OperatorType,
    override val range: SourceRange
) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = UnaryOpNode(
            operand = operand.mapRecursive(mapper),
            operator = operator,
            tokenOperatorType = tokenOperatorType,
            range = range
        )
        return mapper(newNode)
    }
}

data class FuncCallNode(
    val receiver: ExprNode,
    val args: List<ExprNode>,
    val typeNames: List<ExprNode>?,
    override val range: SourceRange
) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            receiver = receiver.mapRecursive(mapper),
            args = args.map { it.mapRecursive(mapper) },
            typeNames = typeNames?.map { it.mapRecursive(mapper) }
        )
        return mapper(newNode)
    }
}

data class IndexAccessNode(
    val target: ExprNode,
    val indexExpr: ExprNode,
    override val range: SourceRange
) : ExprNode {
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
    override val range: SourceRange
) : UnaryOpNode(
    operand = operand,
    operator = UnaryOpType.INCREMENT,
    tokenOperatorType = OperatorType.INCREMENT,
    range = range
)

data class DecrementNode(
    override val operand: ExprNode,
    val isPost: Boolean,
    override val range: SourceRange
) : UnaryOpNode(
    operand = operand,
    operator = UnaryOpType.DECREMENT,
    tokenOperatorType = OperatorType.DECREMENT,
    range = range
)

open class BlockNode(
    open val nodes: List<ExprNode>,
    override val range: SourceRange
) : ExprNode {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = BlockNode(
            nodes = nodes.map { it.mapRecursive(mapper) },
            range = range
        )
        return mapper(newNode)
    }

    companion object {
        fun empty(range: SourceRange) = BlockNode(emptyList(), range)
    }
}

data class LambdaNode(
    val body: BlockNode,
    val params: List<VarDeclStmtNode>?,
    override val range: SourceRange,
) : ExprNode {
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
    override val range: SourceRange
) : BlockNode(nodes, range) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            nodes = nodes.map { it.mapRecursive(mapper) }
        )
        return mapper(newNode)
    }
}

data class ModuleNode(
    val name: String,
//    val path: String?,
    override val nodes: List<ExprNode>,
    override val range: SourceRange
) : BlockNode(
    nodes = nodes,
    range = range
) {
    override fun mapRecursive(mapper: NodeTransformFunc): ExprNode {
        val newNode = this.copy(
            nodes = nodes.map { it.mapRecursive(mapper) }
        )
        return mapper(newNode)
    }
}