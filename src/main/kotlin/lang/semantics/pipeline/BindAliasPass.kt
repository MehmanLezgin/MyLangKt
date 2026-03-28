package lang.semantics.pipeline

import lang.nodes.BlockNode
import lang.nodes.ModuleStmtNode
import lang.nodes.UsingDirectiveNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.resolvers.BaseResolver
import lang.semantics.symbols.ModuleSymbol

class BindAliasPass(
    override val analyzer: ISemanticAnalyzer,
    private val bindImportPass: BindImportPass
) : BaseResolver<BlockNode?, Unit>(analyzer) {
    override fun resolve(target: BlockNode?) {
        target ?: return
        for (node in target.nodes) {
            when (node) {
                is UsingDirectiveNode -> resolve(node)
                is ModuleStmtNode -> resolve(node)
                else -> Unit
            }
        }
    }

    private fun resolve(node: ModuleStmtNode) {
        val moduleSym = node.getResolvedSymbol() as? ModuleSymbol
            ?: return

        analyzer.withScope(moduleSym.scope) {
            resolve(target = node.body)
        }
    }

    fun resolve(node: UsingDirectiveNode) =
        bindImportPass.resolve(node = node)
}