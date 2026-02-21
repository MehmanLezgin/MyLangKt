package lang.semantics.pipeline

import lang.messages.Msg
import lang.messages.Terms
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.resolvers.BaseResolver
import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.*
import lang.semantics.types.ErrorType
import lang.semantics.types.Type

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
                is ModuleStmtNode ->  resolve(target = node)
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

    private fun withScopeResolve(targetScope: Scope, body: BlockNode?) {
        analyzer.withScope(targetScope) {
            resolve(body)
        }
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

    private fun resolve(target: VarDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? VarSymbol ?: return run {
            target.error(Msg.SymbolIsNotRegistered.format(target.name.value))
        }

        if (target.dataType !is AutoDatatypeNode)
            sym.type = analyzer.typeResolver.resolve(target.dataType)
    }

    private fun resolveFuncKind(target: FuncDeclStmtNode): FuncKind {
        return FuncKind.Default(target.name)
        /*
        fun resolveBase(base: ExprNode): Type? {
            val type = analyzer.typeResolver.resolve(base)
            if (type == ErrorType) return null
            if (type.isExprType) {
                base.error(Msg.EXPECTED_TYPE_NAME)
                return null
            }
            return type
        }

        return when (val funcNameExpr = node.name) {
            is IdentifierNode -> {
                FuncKind.Default(funcNameExpr)
            }

            is DotAccessNode -> {
                val base = funcNameExpr.base
                val nameId = funcNameExpr.member
                val type = resolveBase(base) ?: return null
                FuncKind.Extension(nameId, type)
            }

            is ScopedDatatypeNode -> {
                val base = funcNameExpr.base
                if (!funcNameExpr.member.isSimple()) {
                    funcNameExpr.member.error(Msg.EXPECTED_X_NAME.format(Terms.FUNCTION))
                    return null
                }

                val nameId = funcNameExpr.member.identifier
                val type = resolveBase(base) ?: return null
                FuncKind.Qualified(nameId, type)
            }

            else -> {
                funcNameExpr.error(Msg.EXPECTED_X_NAME.format(Terms.FUNCTION))
                null
            }
        }*/

    }


    fun resolve(target: FuncDeclStmtNode) {
        val modifiers = modResolver.resolveFuncModifiers(target.modifiers)

        val funcKind = resolveFuncKind(target)

        fun checkInfix() {
            if (!modifiers.isInfix) return
            val infixAllowed = funcKind is FuncKind.Extension || scope.isTypeScope()
            if (infixAllowed) return
            val modNode = target.modifiers?.get(ModifierNode.Infix::class) ?: return
            val msg = Msg.F_MODIFIER_IS_NOT_INAPPLICABLE_ON_THIS_X.format(
                modNode.keyword.value, Terms.FUNCTION
            )
            modNode.error(msg)
        }

        checkInfix()

        val returnType = analyzer.typeResolver.resolve(target.returnType)

        val params = target.params.map { decl ->
            resolveFuncParam(target = decl)
        }.let {
            FuncParamListSymbol(list = it.toList())
        }

        withEffectiveScope(modifiers.isStatic)
        {
            val result = scope.defineFunc(target, target.name, params, returnType, modifiers)

            result.handle(target.range) {
                if (sym !is FuncSymbol) return@handle null

                target bind sym
            }
        }
    }

    private fun resolveFuncParam(target: VarDeclStmtNode): FuncParamSymbol {
        val type = when {
            target.dataType is AutoDatatypeNode -> {
                target.error(Msg.EXPECTED_TYPE_NAME)
                ErrorType
            }

            else -> {
                when (val type = analyzer.typeResolver.resolve(target.dataType)) {
                    PrimitivesScope.void -> {
                        semanticError(Msg.VOID_CANNOT_BE_PARAM_TYPE, target.name.range)
                        ErrorType
                    }

                    else -> type
                }
            }
        }

        val sym = FuncParamSymbol(
            name = target.name.value,
            type = type,
            defaultValue = target.initializer
        )

        target bind sym

        return sym
    }

    private fun <T> withEffectiveScope(isStatic: Boolean, block: () -> T): T {
        return if (!isStatic && scope.isTypeScope())
            analyzer.withScope((scope as BaseTypeScope).instanceScope, block)
        else
            block()
    }

}