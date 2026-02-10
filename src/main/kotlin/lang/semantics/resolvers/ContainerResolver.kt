package lang.semantics.resolvers

import lang.nodes.*
import lang.semantics.ISemanticAnalyzer

class ContainerResolver(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<BlockNode, Unit>(analyzer = analyzer) {
    override fun resolve(target: BlockNode) {
        target.nodes.forEach { node ->
            when (node) {
                is ClassDeclStmtNode -> resolveClass(target = node)
                is InterfaceDeclStmtNode -> resolveInterface(target = node)
                is EnumDeclStmtNode -> resolveEnum(target = node)
                is UsingDirectiveNode -> resolveUsing(target = node)
                else -> Unit
            }
        }
    }

    private fun resolveInterface(target: InterfaceDeclStmtNode) {
        val modifiers = analyzer.modResolver.resolve(target.modifiers)
        scope.defineInterface(target, modifiers = modifiers)
            .handle(target.name.range) {
                target bind sym
            }
    }

    private fun resolveClass(target: ClassDeclStmtNode) {
        val modifiers = analyzer.modResolver.resolve(target.modifiers)
        scope.defineClass(target, modifiers = modifiers)
            .handle(target.name.range) {
                target bind sym
            }
    }

    private fun resolveEnum(target: EnumDeclStmtNode) {
        val modifiers = analyzer.modResolver.resolve(target.modifiers)
        scope.defineEnum(target, modifiers = modifiers)
            .handle(target.name.range) {
                target bind sym
            }
    }

    private fun resolveUsing(target: UsingDirectiveNode) {
        if (!target.isType) return
        val name = target.name ?: return
        val modifiers = analyzer.modResolver.resolve(target.modifiers)
        scope.defineUsing(name = name.value, visibility = modifiers.visibility)
            .handle(target.name.range) {
                target bind sym
            }
    }
}