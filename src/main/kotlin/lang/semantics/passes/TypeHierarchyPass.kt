package lang.semantics.passes

import lang.messages.Msg
import lang.messages.Terms
import lang.nodes.BaseDatatypeNode
import lang.nodes.BlockNode
import lang.nodes.ClassDeclStmtNode
import lang.nodes.EnumDeclStmtNode
import lang.nodes.InterfaceDeclStmtNode
import lang.nodes.ModuleStmtNode
import lang.nodes.UsingDirectiveNode
import lang.nodes.VoidDatatypeNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.resolvers.BaseResolver
import lang.semantics.scopes.Scope
import lang.semantics.symbols.ClassSymbol
import lang.semantics.symbols.EnumSymbol
import lang.semantics.symbols.InterfaceSymbol
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.TypeSymbol
import lang.semantics.types.Type

class TypeHierarchyPass(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<BlockNode?, Unit>(analyzer = analyzer) {
    override fun resolve(target: BlockNode?) {
        target?.nodes?.forEach { node ->
            when (node) {
                is ClassDeclStmtNode -> resolveClass(target = node)
                is InterfaceDeclStmtNode -> resolveInterface(target = node)
                is EnumDeclStmtNode -> resolveEnum(target = node)
                is ModuleStmtNode ->  resolveModule(target = node)
                else -> Unit
            }
        }
    }

    private fun withScopeResolve(targetScope: Scope, body: BlockNode?) {
        analyzer.withScope(targetScope) {
            resolve(body)
        }
    }

    private fun resolveEnum(target: EnumDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? EnumSymbol ?: return
        withScopeResolve(sym.scope, target.body)
    }

    private fun resolveModule(target: ModuleStmtNode) {
        val sym = target.getResolvedSymbol() as? ModuleSymbol ?: return
        withScopeResolve(sym.scope, target.body)
    }

    private fun resolveInterface(target: InterfaceDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? InterfaceSymbol ?: return

        val superType = resolveSuperType(target.superInterface)

        if (superType != null && superType.declaration !is InterfaceSymbol) {
            semanticError(Msg.INTERFACE_CAN_EXTEND_INTERFACE, target.superInterface?.range)
        }

        sym.superType = superType

        withScopeResolve(sym.scope, target.body)
    }

    private fun resolveClass(target: ClassDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? ClassSymbol ?: return

        val superType = resolveSuperType(target.superClass)

        val decl = superType?.declaration

        if (decl != null && (decl !is InterfaceSymbol && decl !is ClassSymbol)) {
            semanticError(Msg.CLASS_CAN_EXTEND_INTERFACE_OR_CLASS, target.superClass?.range)
        }

        sym.superType = superType
        withScopeResolve(sym.scope, target.body)
    }

    private fun resolveSuperType(
        superType: BaseDatatypeNode?
    ): Type? {
        if (superType == null || superType is VoidDatatypeNode) return null
        val type = analyzer.typeResolver.resolve(superType)

        if (type.declaration?.modifiers?.isOpen == false)
            semanticError(
                Msg.F_MUST_BE_OPEN_TYPE.format(
                    type.declaration?.name ?: Terms.SYMBOL
                ), superType.range
            )

        return type
    }

}