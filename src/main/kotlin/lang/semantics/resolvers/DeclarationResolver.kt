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

    private fun resolve(target: UsingDirectiveNode) {
        val value = target.value.let {
            if (it is IdentifierNode) it.toDatatype() else it
        }

        val name = target.name

        fun getNamespaceSym(target: BaseDatatypeNode): TypeSymbol? {
            val type = analyzer.typeResolver.resolve(target, isNamespaceCtx = true)
            if (type == ErrorType) return null
            val decl = type.declaration
            if (decl !is TypeSymbol) {
                target.error(Msg.EXPECTED_MODULE_NAME)
                return null
            }
            return decl
        }

        val modifiers = analyzer.modResolver.resolveUsingModifiers(target.modifiers)
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

            else -> target.error(Msg.EXPECTED_MODULE_NAME)
        }
    }

    private fun Scope.defineDeclSym(target: DeclStmtNode): Symbol? {
        val modResolver = analyzer.modResolver
        val modNode = target.modifiers

        val result = when (target) {
            is InterfaceDeclStmtNode -> {
                val modifiers = modResolver.resolveInterfaceModifiers(modNode)
                val sym = this.defineInterface(target, modifiers)
                analyzer.declarationHeaderPass.resolve(target)
                sym
            }

            is ClassDeclStmtNode -> {
                val modifiers = modResolver.resolveClassModifiers(modNode)
                val sym = this.defineClass(target, modifiers)
                analyzer.declarationHeaderPass.resolve(target)
                sym
            }

            is EnumDeclStmtNode -> {
                val modifiers = modResolver.resolveEnumModifiers(modNode)
                val sym = this.defineEnum(target, modifiers)
                analyzer.declarationHeaderPass.resolve(target)
                sym
            }

            else -> {
                target.error(Msg.EXPECTED_A_DECLARATION)
                null
            }
        }

        val sym = result?.handle(target.range) { sym }

        return sym
    }

    private fun Scope.ensureDeclared(target: DeclStmtNode): Symbol? {
        target.getResolvedSymbol()?.let { return it }

        if (kind == ScopeKind.CONTAINER)
            target.error(Msg.SymbolIsNotRegistered.format(target.name?.value!!))

        val sym = scope.defineDeclSym(target)

        target bind sym

        return sym
    }

    private fun resolve(target: ModuleStmtNode) {
        val moduleSym = target.getResolvedSymbol() as? ModuleSymbol
            ?: return

        analyzer.withScopeResolveBody(moduleSym.scope, target.body)
    }

    private fun resolve(target: InterfaceDeclStmtNode) {
        val sym = scope.ensureDeclared(target) as InterfaceSymbol
        analyzer.withScopeResolveBody(targetScope = sym.scope, body = target.body)
    }

    private fun resolve(target: ClassDeclStmtNode) {
        val sym = scope.ensureDeclared(target) as ClassSymbol
        analyzer.withScopeResolveBody(targetScope = sym.scope, body = target.body)
    }

    private fun resolve(target: EnumDeclStmtNode) {
        val sym = scope.ensureDeclared(target) as EnumSymbol
        analyzer.withScopeResolveBody(targetScope = sym.scope, body = target.body)
    }

    private fun resolve(target: VarDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? VarSymbol ?: return

        var type = sym.type

        if (target.initializer == null && type is UnresolvedType) {
            target.error(Msg.VarMustBeInitialized.format(sym.name))
            return
        }

        if (target.initializer != null) {
            analyzer.typeResolver.resolveForType(target.initializer, type)
                .takeIf { it != ErrorType }
                ?.let {
                    if (sym.type is UnresolvedType) {
                        sym.type = it
                        type = it
                    }

                }

            target.initializer attach sym.type
        }

        val isConst = sym.type.isConst

        if (isConst && (!sym.modifiers.isStatic && (scope.isTypeScope()))) {
            target.error(Msg.CONST_VAR_MUST_BE_STATIC)
        }

        val initSym = target.initializer?.getResolvedSymbol()

        if (initSym is ConstValueSymbol) {
            var value = initSym.value

            if (value != null && value.type != type)
                value = analyzer.constResolver.resolveCast(value, type)

            sym.constValue = value
        }
    }

    private fun resolve(target: FuncDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? FuncSymbol ?: return

        val funcScope = FuncScope(
            parent = scope,
            funcSymbol = sym
        )

        analyzer.withScopeResolveBody(targetScope = funcScope, body = target.body)
    }

    private fun resolve(target: ConstructorDeclStmtNode) {
        if (scope !is ClassScope)
            semanticError(Msg.CONSTRUCTOR_OUTSIDE_CLASS_ERROR, target.range)

        resolve(target as FuncDeclStmtNode)
    }

    private fun resolve(target: DestructorDeclStmtNode) {
        if (scope !is ClassScope)
            semanticError(Msg.DESTRUCTOR_OUTSIDE_CLASS_ERROR, target.range)

        resolve(target as FuncDeclStmtNode)
    }
//    fun ScopeResult.handle(onSuccess: ScopeResult.Success<*>.() -> Unit) =
//        this.handle(null, onSuccess)
}