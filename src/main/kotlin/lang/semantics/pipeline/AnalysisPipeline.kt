package lang.semantics.pipeline

import lang.compiler.SourceUnit
import lang.nodes.BlockNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.resolvers.BaseResolver

class AnalysisPipeline(
    val analyzer: ISemanticAnalyzer,
    val moduleRegPass: ModuleRegPass,
    val passes: List<BaseResolver<BlockNode?, *>>
) {
    fun execute(sources: List<SourceUnit>) {
        sources.forEach {
            it.scope = moduleRegPass.resolve(target = it.ast)
        }

        passes.forEach { pass ->
            sources.forEach {
                analyzer.withScope(it.scope!!) {
                    pass.resolve(target = it.ast)
                }
            }
        }
    }
}