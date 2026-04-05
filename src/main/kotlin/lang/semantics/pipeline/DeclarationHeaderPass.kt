package lang.semantics.pipeline

import lang.core.PrimitivesScope
import lang.messages.Msg
import lang.messages.Terms
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.resolvers.BaseResolver
import lang.semantics.scopes.Scope
import lang.semantics.symbols.*
import lang.semantics.types.ErrorType
import lang.semantics.types.Type
import lang.semantics.types.UnresolvedType

class DeclarationHeaderPass(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<BlockNode?, Unit>(analyzer = analyzer) {
    private val modResolver = analyzer.modResolver

    override fun resolve(target: BlockNode?) {
        target?.nodes?.forEach { node ->
            when (node) {
                is ClassDeclStmtNode -> resolve(node = node)
                is InterfaceDeclStmtNode -> resolve(node = node)
                is EnumDeclStmtNode -> resolve(node = node)
                is ModuleStmtNode -> resolve(node = node)
                is VarDeclStmtNode -> resolve(node = node)
                is FuncDeclStmtNode -> resolve(node = node)
                else -> Unit
            }
        }
    }

    fun resolve(node: EnumDeclStmtNode) {
        val sym = node.getResolvedSymbol() as? EnumSymbol ?: return
        withScopeResolve(sym.scope, node.body)
    }

    fun resolve(node: ModuleStmtNode) {
        val sym = node.getResolvedSymbol() as? ModuleSymbol ?: return
        withScopeResolve(sym.scope, node.body)
    }


    fun resolve(node: InterfaceDeclStmtNode) {
        val sym = node.getResolvedSymbol() as? InterfaceSymbol ?: return

        val superType = resolveSuperType(node.superInterface)

        if (superType != null && superType.declaration !is InterfaceSymbol) {
            semanticError(Msg.INTERFACE_CAN_EXTEND_INTERFACE, node.superInterface?.range)
        }

        sym.superType = superType

        withScopeResolve(sym.scope, node.body)
    }

    fun resolve(node: ClassDeclStmtNode) {
        val sym = node.getResolvedSymbol() as? ClassSymbol ?: return

        val superType = resolveSuperType(node.superClass)

        val decl = superType?.declaration

        if (decl != null && (decl !is InterfaceSymbol && decl !is ClassSymbol)) {
            semanticError(Msg.CLASS_CAN_EXTEND_INTERFACE_OR_CLASS, node.superClass?.range)
        }

        sym.superType = superType
        sym.staticScope.superTypeScope = decl?.staticScope
        withScopeResolve(sym.scope, node.body)
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

    fun resolve(node: VarDeclStmtNode) {
        val sym = node.getResolvedSymbol() as? VarSymbol ?: return

        if (node.datatype is AutoDatatypeNode) return

        var type = analyzer.typeResolver.resolve(node.datatype)

        if (type.isExprType) {
            type = ErrorType
            node.datatype.error(Msg.EXPECTED_TYPE_NAME)
        }

        sym.type = type
    }

    fun resolve(node: FuncDeclStmtNode) {
        val modifiers = modResolver.resolveFuncModifiers(node.modifiers)

        val returnType = when (node.returnType) {
            is AutoDatatypeNode -> {
                when (node.body) {
                    null -> PrimitivesScope.void
                    else -> UnresolvedType
                }
            }

            else -> analyzer.typeResolver.resolve(node.returnType)
        }

        val params = analyzer.typeResolver.resolveFuncParams(params = node.params)

        withEffectiveScope(modifiers.isStatic) {
            val result = scope.defineFunc(
                node = node,
                nameId = node.name,
                params = params,
                returnType = returnType,
                modifiers = modifiers
            )

            result.handle(node.range) {
                if (sym !is FuncSymbol) return@handle null

                node bind sym
            }
        }
    }

    private fun withScopeResolve(targetScope: Scope, body: BlockNode?) {
        analyzer.withScope(targetScope) {
            resolve(body)
        }
    }
}