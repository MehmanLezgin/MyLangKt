package lang.semantics.resolvers

import lang.messages.Msg
import lang.messages.Terms
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.builtin.PrimitivesScope
import lang.semantics.scopes.*
import lang.semantics.symbols.*
import lang.semantics.types.ConstValue
import lang.semantics.types.ErrorType
import lang.semantics.types.OverloadedFuncType
import lang.semantics.types.Type
import lang.core.SourceRange
import lang.semantics.types.PointerType

class DeclarationResolver(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<DeclStmtNode<*>, Unit>(analyzer = analyzer) {
    override fun resolve(target: DeclStmtNode<*>) {
        when (target) {
            is VarDeclStmtNode -> resolve(target)
            is ConstructorDeclStmtNode -> resolve(target)
            is DestructorDeclStmtNode -> resolve(target)
            is FuncDeclStmtNode -> resolve(target)
            is InterfaceDeclStmtNode -> resolve(target)
            is ClassDeclStmtNode -> resolve(target)
            is EnumDeclStmtNode -> resolve(target)
            is TypedefStmtNode -> resolve(target)
            is NamespaceStmtNode -> resolve(target)
        }
    }

    private fun <T : Symbol> T.bindAndExport(node: ExprNode, isExport: Boolean): T {
        node bind this
        exportIfNeeded(isExport)
        return this
    }

    private fun <T : Symbol> T.exportIfNeeded(isExport: Boolean): T {
        if (isExport)
            analyzer.exportSymbol(this)
        return this
    }

    private fun resolve(node: NamespaceStmtNode) {
        val modifiers = analyzer.modResolver.resolveNamespaceModifiers(node.modifiers)

        val result = scope.defineNamespace(node, isExport = modifiers.isExport)

        result.handle(node.range) {
            sym.bindAndExport(node, modifiers.isExport)
            if (sym !is NamespaceSymbol) return@handle null
            analyzer.withScopeResolveBody(targetScope = sym.scope, body = node.body)
        }
    }

    private fun resolve(node: TypedefStmtNode) {
        val modifiers = analyzer.modResolver.resolveTypedefModifiers(node.modifiers)

        val type = analyzer.typeResolver.resolve(node.dataType)
        node attach type
        if (type is ErrorType) return
        val result = scope.defineTypedef(node, type)
        result.handle(node.range) {
            sym.bindAndExport(node, modifiers.isExport)
        }
    }

    private fun resolveAutoVarType(node: VarDeclStmtNode): Type {
        if (node.initializer == null) {
            semanticError(Msg.EXPECTED_TYPE_NAME, node.name.range)
            return ErrorType
        }

        val initType = analyzer.typeResolver.resolve(node.initializer)

        val sym = node.initializer.getResolvedSymbol()

        when {
            sym is ConstValueSymbol ->
                return initType.setFlags(isConst = false)

            initType is OverloadedFuncType ->
                node.initializer.error(Msg.AMBIGUOUS_OVERLOADED_FUNCTION)

            else -> return initType
        }

        return ErrorType
    }

    private fun resolveVarType(node: VarDeclStmtNode): Type {
        if (node.dataType is AutoDatatypeNode) {
            val type = resolveAutoVarType(node)
            return type
        }

        var type = analyzer.typeResolver.resolve(node.dataType)

        if (type.isExprType) {
            type = ErrorType
            node.dataType.error(Msg.EXPECTED_TYPE_NAME)
        }

        if (node.initializer == null)
            return type

        analyzer.typeResolver.resolveForType(node.initializer, type)
            .takeIf { it != ErrorType }
            ?.let { type = it }

        node.initializer attach type
        return type
    }

    private fun resolveConstVar(node: VarDeclStmtNode, type: Type, modifiers: Modifiers) {
        var constValue: ConstValue<*>? = null

        if (type.isConst) {
            val sym = node.initializer?.getResolvedSymbol()

            if (sym is ConstValueSymbol) {
                var value = sym.value

                if (value != null && value.type != type)
                    value = analyzer.constResolver.resolveCast(value, type)

                constValue = value
            }
        }

//        if (constValue == null) return
        withEffectiveScope(modifiers.isStatic) {
            val result = scope.defineConstVar(node, type, constValue, modifiers)
            result.handle(node.range) {
                sym.bindAndExport(node, modifiers.isExport)
            }
        }
    }

    private fun resolve(node: VarDeclStmtNode) {
        val modifiers = analyzer.modResolver.resolveVarModifiers(node.modifiers)

        val type = resolveVarType(node)
        node attach type

        val isConst = type.isConst// || modifiers.isConst

        if (isConst && (!modifiers.isStatic && (scope.isTypeScope()))) {
            node.error(Msg.CONST_VAR_MUST_BE_STATIC)
        }

        if (!isConst || type is PointerType) {
            withEffectiveScope(modifiers.isStatic) {
                val result = scope.defineVar(node, type, modifiers)
                result.handle(node.range) {
                    sym.bindAndExport(node, modifiers.isExport)
                }
            }

            return
        }

        resolveConstVar(node, type, modifiers)
    }

    private fun <T> withEffectiveScope(isStatic: Boolean, block: () -> T): T {
        return if (!isStatic && scope.isTypeScope())
            analyzer.withScope((scope as BaseTypeScope).instanceScope, block)
        else
            block()
    }

    private fun resolveFuncParam(node: VarDeclStmtNode) {
        val paramsScope = scope

        if (paramsScope !is FuncParamsScope) {
            semanticError(Msg.CANNOT_RESOLVE_PARAM_OUTSIDE_FUNC, node.range)
            return
        }

        val type = analyzer.typeResolver.resolve(node.dataType)

        val result = paramsScope.defineParam(node, type)
        result.handle(node.range) {
            sym.also { node bind it }
        }

        if (node.dataType is AutoDatatypeNode)
            semanticError(Msg.EXPECTED_TYPE_NAME, node.name.range)
        else if (type == PrimitivesScope.void)
            semanticError(Msg.VOID_CANNOT_BE_PARAM_TYPE, node.name.range)

        analyzer.typeResolver.resolve(node.dataType)
    }

    private fun resolveFuncKind(node: FuncDeclStmtNode): FuncKind? {
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
        }
    }

    private fun checkInfix() {

    }

    private fun resolve(node: FuncDeclStmtNode) {
        val modifiers = analyzer.modResolver.resolveFuncModifiers(node.modifiers)

        val paramsScope = FuncParamsScope(
            parent = scope
        )

        val funcKind = resolveFuncKind(node)

        when (funcKind) {
            is FuncKind.Default -> {}
            is FuncKind.Extension -> {}
            is FuncKind.Qualified -> {}
            null -> return
        }

        val funcNameId = funcKind.nameId

        fun checkInfix() {
            if (!modifiers.isInfix) return
            val infixAllowed = funcKind is FuncKind.Extension || scope.isTypeScope()
            if (infixAllowed) return
            val modNode = node.modifiers?.get(ModifierNode.Infix::class) ?: return
            val msg = Msg.F_MODIFIER_IS_NOT_INAPPLICABLE_ON_THIS_X.format(
                modNode.keyword.value, Terms.FUNCTION
            )
            modNode.error(msg)
        }

        checkInfix()

        val params = analyzer.withScope(paramsScope) {
            if (funcKind is FuncKind.Extension)
                paramsScope.define(
                    FuncExtensionParamSymbol(type = funcKind.type)
                )

            node.params.forEach { decl ->
                resolveFuncParam(node = decl)
            }

            return@withScope paramsScope.getParams()
        }

        val returnType = analyzer.typeResolver.resolve(node.returnType)


        withEffectiveScope(modifiers.isStatic)
        {
            val pair = scope.defineFunc(node, funcNameId, params, returnType, modifiers)

            pair.second.exportIfNeeded(modifiers.isExport)
            val result = pair.first

            result.handle(node.range) {
                if (sym !is FuncSymbol) return@handle null

                val funcScope = FuncScope(
                    parent = scope,
                    funcSymbol = sym
                )

                val funcType = sym.toFuncType()

                node bind sym
                node attach funcType

                analyzer.withScopeResolveBody(targetScope = funcScope, body = node.body)
            }
        }

    }

    private fun resolve(node: ConstructorDeclStmtNode) {
        if (scope !is ClassScope)
            semanticError(Msg.CONSTRUCTOR_OUTSIDE_CLASS_ERROR, node.range)

        resolve(node as FuncDeclStmtNode)
    }

    private fun resolve(node: DestructorDeclStmtNode) {
        if (scope !is ClassScope)
            semanticError(Msg.DESTRUCTOR_OUTSIDE_CLASS_ERROR, node.range)

        resolve(node as FuncDeclStmtNode)
    }

    private fun resolve(node: InterfaceDeclStmtNode) {
        val modifiers = analyzer.modResolver.resolveInterfaceModifiers(node.modifiers)

        val superType = resolveSuperType(node.superInterface)

        if (superType != null && superType.declaration !is InterfaceSymbol) {
            semanticError(Msg.INTERFACE_CAN_EXTEND_INTERFACE, node.superInterface?.range)
        }

        val result = scope.defineInterface(node, modifiers, superType)

        result.handle(node.range) {
            sym.bindAndExport(node, modifiers.isExport)
            if (sym !is InterfaceSymbol) return@handle null
            analyzer.withScopeResolveBody(targetScope = sym.scope, body = node.body)
        }
    }

    private fun resolve(node: ClassDeclStmtNode) {
        val modifiers = analyzer.modResolver.resolveClassModifiers(node.modifiers)

        val superType = resolveSuperType(node.superClass)

        if (superType != null &&
            superType.declaration !is InterfaceSymbol &&
            superType.declaration !is ClassSymbol
        )
            semanticError(Msg.CLASS_CAN_EXTEND_INTERFACE_OR_CLASS, node.superClass?.range)

        val result = scope.defineClass(node, modifiers, superType)
        result.handle(node.range) {
            sym.bindAndExport(node, modifiers.isExport)
            if (sym !is ClassSymbol) return@handle null
            analyzer.withScopeResolveBody(targetScope = sym.scope, body = node.body)
        }

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

    private fun resolve(node: EnumDeclStmtNode) {
        val modifiers = analyzer.modResolver.resolveEnumModifiers(node.modifiers)
        val result = scope.defineEnum(node, modifiers)
        result.handle(node.range) {
            sym.bindAndExport(node, modifiers.isExport)
            if (sym !is EnumSymbol) return@handle null
            analyzer.withScopeResolveBody(targetScope = sym.scope, body = node.body)
        }
    }


    fun <T> ScopeResult.handle(range: SourceRange?, onSuccess: ScopeResult.Success<*>.() -> T?): T? {
        return when (this) {
            is ScopeResult.Error -> {
                if (range != null)
                    analyzer.scopeError(error, range)
                null
            }

            is ScopeResult.Success<*> -> {
                onSuccess()
            }
        }
    }

    fun Scope.isTypeScope() = scope is BaseTypeScope && scope !is NamespaceScope

//    fun ScopeResult.handle(onSuccess: ScopeResult.Success<*>.() -> Unit) =
//        this.handle(null, onSuccess)
}