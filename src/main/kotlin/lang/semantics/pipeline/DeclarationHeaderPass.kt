package lang.semantics.pipeline

import lang.messages.Msg
import lang.messages.Terms
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.core.PrimitivesScope
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
                is ClassDeclStmtNode -> resolve(target = node)
                is InterfaceDeclStmtNode -> resolve(target = node)
                is EnumDeclStmtNode -> resolve(target = node)
                is ModuleStmtNode -> resolve(target = node)
                is VarDeclStmtNode -> resolve(target = node)
                is FuncDeclStmtNode -> resolve(target = node)
                else -> Unit
            }
        }
    }


    fun resolve(target: EnumDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? EnumSymbol ?: return
        withScopeResolve(sym.scope, target.body)
    }

    fun resolve(target: ModuleStmtNode) {
        val sym = target.getResolvedSymbol() as? ModuleSymbol ?: return
        withScopeResolve(sym.scope, target.body)
    }


    fun resolve(target: InterfaceDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? InterfaceSymbol ?: return

        val superType = resolveSuperType(target.superInterface)

        if (superType != null && superType.declaration !is InterfaceSymbol) {
            semanticError(Msg.INTERFACE_CAN_EXTEND_INTERFACE, target.superInterface?.range)
        }

        sym.superType = superType

        withScopeResolve(sym.scope, target.body)
    }

    fun resolve(target: ClassDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? ClassSymbol ?: return

        val superType = resolveSuperType(target.superClass)

        val decl = superType?.declaration

        if (decl != null && (decl !is InterfaceSymbol && decl !is ClassSymbol)) {
            semanticError(Msg.CLASS_CAN_EXTEND_INTERFACE_OR_CLASS, target.superClass?.range)
        }

        sym.superType = superType
        sym.staticScope.superTypeScope = decl?.staticScope
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

    fun resolve(target: VarDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? VarSymbol ?: return

        if (target.datatype is AutoDatatypeNode) return

        var type = analyzer.typeResolver.resolve(target.datatype)

        if (type.isExprType) {
            type = ErrorType
            target.datatype.error(Msg.EXPECTED_TYPE_NAME)
        }

        sym.type = type
    }

    fun resolve(target: FuncDeclStmtNode) {
        val modifiers = modResolver.resolveFuncModifiers(target.modifiers)

        val returnType = when (target.returnType) {
            is AutoDatatypeNode -> {
                when (target.body) {
                    null -> PrimitivesScope.void
                    else -> UnresolvedType
                }
            }
            else -> analyzer.typeResolver.resolve(target.returnType)
        }

        val params = analyzer.typeResolver.resolveFuncParams(params = target.params)

        withEffectiveScope(modifiers.isStatic) {
            val result = scope.defineFunc(
                node = target,
                nameId = target.name,
                params = params,
                returnType = returnType,
                modifiers = modifiers
            )

            result.handle(target.range) {
                if (sym !is FuncSymbol) return@handle null

                target bind sym
            }
        }
    }

    private fun withScopeResolve(targetScope: Scope, body: BlockNode?) {
        analyzer.withScope(targetScope) {
            resolve(body)
        }
    }
}