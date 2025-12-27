package lang.semantics

import lang.nodes.BlockNode
import lang.nodes.ExprNode

interface ISemanticAnalyzer {
    fun resolve(node: ExprNode)
    fun resolve(node: BlockNode)
}