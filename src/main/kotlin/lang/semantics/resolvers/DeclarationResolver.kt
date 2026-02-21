package lang.semantics.resolvers

import lang.messages.Msg
import lang.nodes.*
import lang.parser.ParserUtils.toDatatype
import lang.semantics.ISemanticAnalyzer
import lang.semantics.scopes.*
import lang.semantics.symbols.*
import lang.semantics.types.*

class DeclarationResolver(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<DeclStmtNode, Unit>(analyzer = analyzer) {
    override fun resolve(target: DeclStmtNode) {
        when (target) {
            is ModuleStmtNode -> resolve(target)
            is InterfaceDeclStmtNode -> resolve(target)
            is EnumDeclStmtNode -> resolve(target)
            is ClassDeclStmtNode -> resolve(target)

            is VarDeclStmtNode -> resolve(target)
            is ConstructorDeclStmtNode -> resolve(target)
            is DestructorDeclStmtNode -> resolve(target)
            is FuncDeclStmtNode -> resolve(target)
            is UsingDirectiveNode -> resolve(target)
        }
    }

    private fun resolve(node: UsingDirectiveNode) {
        val value = node.value.let {
            if (it is IdentifierNode) it.toDatatype() else it
        }

        val name = node.name

        fun getNamespaceSym(node: BaseDatatypeNode): TypeSymbol? {
            val type = analyzer.typeResolver.resolve(node, isNamespaceCtx = true)
            if (type == ErrorType) return null
            val decl = type.declaration
            if (decl !is TypeSymbol) {
                node.error(Msg.EXPECTED_MODULE_NAME)
                return null
            }
            return decl
        }

        val modifiers = analyzer.modResolver.resolveUsingModifiers(node.modifiers)
        val visibility = modifiers.visibility

        fun defineUsingSymbol(name: String?, sym: Symbol) {
            scope.defineUsing(name ?: sym.name, visibility)
                .handle(value.range) {}
        }

        fun defineUsingNamespace(namespaceScope: BaseTypeScope) {
            namespaceScope.symbols.forEach { (_, memberSym) ->
                scope.define(memberSym, visibility)
                    .handle(value.range) {}
            }
        }

        when (value) {
            is DatatypeNode -> {
                val namespaceSym = getNamespaceSym(value) ?: return
                defineUsingNamespace(namespaceScope = namespaceSym.staticScope)
            }

            is ScopedDatatypeNode -> {
                val namespaceSym = getNamespaceSym(value.base) ?: return
                val targetScope = namespaceSym.staticScope
                val memberName = value.member.identifier

                val type = analyzer.withScope(targetScope) {
                    analyzer.typeResolver
                        .resolve(memberName, asMember = true, isNamespace = true)
                }

                if (type == ErrorType) return

                targetScope.resolve(memberName.value).handle(value.range) {
                    if (sym is TypeSymbol)
                        defineUsingNamespace(sym.staticScope)
                    else
                        defineUsingSymbol(name?.value, sym)

                }
            }

            else -> node.error(Msg.EXPECTED_MODULE_NAME)
        }
    }

    private fun Scope.defineDeclSym(node: DeclStmtNode): Symbol? {
        val modResolver = analyzer.modResolver
        val modNode = node.modifiers

        val result = when (node) {
            is InterfaceDeclStmtNode -> {
                val modifiers = modResolver.resolveInterfaceModifiers(modNode)
                val sym = this.defineInterface(node, modifiers)
                analyzer.declarationHeaderPass.resolve(node)
                sym
            }

            is ClassDeclStmtNode -> {
                val modifiers = modResolver.resolveClassModifiers(modNode)
                val sym = this.defineClass(node, modifiers)
                analyzer.declarationHeaderPass.resolve(node)
                sym
            }

            is EnumDeclStmtNode -> {
                val modifiers = modResolver.resolveEnumModifiers(modNode)
                val sym = this.defineEnum(node, modifiers)
                analyzer.declarationHeaderPass.resolve(node)
                sym
            }

            else -> {
                node.error(Msg.EXPECTED_A_DECLARATION)
                null
            }
        }

        val sym = result?.handle(node.range) { sym }

        return sym
    }

    private fun Scope.ensureDeclared(node: DeclStmtNode): Symbol? {
        node.getResolvedSymbol()?.let { return it }

        if (kind == ScopeKind.CONTAINER)
            node.error(Msg.SymbolIsNotRegistered.format(node.name?.value!!))

        val sym = scope.defineDeclSym(node)

        node bind sym

        return sym
    }

    private fun resolve(node: ModuleStmtNode) {
        val moduleSym = node.getResolvedSymbol() as? ModuleSymbol
            ?: return

        analyzer.withScopeResolveBody(moduleSym.scope, node.body)
    }

    private fun resolve(node: InterfaceDeclStmtNode) {
        val sym = scope.ensureDeclared(node) as InterfaceSymbol
        analyzer.withScopeResolveBody(targetScope = sym.scope, body = node.body)
    }

    private fun resolve(node: ClassDeclStmtNode) {
        val sym = scope.ensureDeclared(node) as ClassSymbol
        analyzer.withScopeResolveBody(targetScope = sym.scope, body = node.body)
    }

    private fun resolve(node: EnumDeclStmtNode) {
        val sym = scope.ensureDeclared(node) as EnumSymbol
        analyzer.withScopeResolveBody(targetScope = sym.scope, body = node.body)
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

        withEffectiveScope(modifiers.isStatic) {
            val result = scope.defineConstVar(node, type, constValue, modifiers)
            result.handle(node.range) {
                node bind sym
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
                    node bind sym
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

    private fun resolve(node: FuncDeclStmtNode) {
        val sym = node.getResolvedSymbol() as? FuncSymbol ?: return run {
            node.error(Msg.SymbolIsNotRegistered.format(node.name.value))
        }

        val funcScope = FuncScope(
            parent = scope,
            funcSymbol = sym
        )

        analyzer.withScopeResolveBody(targetScope = funcScope, body = node.body)
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
//    fun ScopeResult.handle(onSuccess: ScopeResult.Success<*>.() -> Unit) =
//        this.handle(null, onSuccess)
}