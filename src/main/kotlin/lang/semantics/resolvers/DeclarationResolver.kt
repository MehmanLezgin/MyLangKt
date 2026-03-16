package lang.semantics.resolvers

import lang.messages.Msg
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.scopes.ClassScope
import lang.semantics.scopes.FuncScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.*

class DeclarationResolver(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<BaseDeclStmtNode, Unit>(analyzer = analyzer) {
    override fun resolve(target: BaseDeclStmtNode) {
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
            else -> {}
        }
    }

    private fun resolve(target: UsingDirectiveNode) {
        analyzer.localDeclPipeline.execute(target)
    }

    /*
        private fun resolve(target: UsingDirectiveNode) {
            val value = target.value.let {
                if (it is IdentifierNode) it.toDatatype() else it
            }

            val name = target.name

            fun getModuleSym(target: BaseDatatypeNode): TypeSymbol? {
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

            fun defineUsingModule(moduleScope: BaseTypeScope) {
                moduleScope.symbols.forEach { (_, memberSym) ->
                    scope.define(memberSym, visibility)
                        .handle(value.range) {}
                }
            }

            when (value) {
                is DatatypeNode -> {
                    val moduleSym = getModuleSym(value) ?: return
                    defineUsingModule(moduleScope = moduleSym.staticScope)
                }

                is ScopedDatatypeNode -> {
                    val moduleSym = getModuleSym(value.base) ?: return
                    val targetScope = moduleSym.staticScope
                    val memberName = value.member.identifier

                    val type = analyzer.withScope(targetScope) {
                        analyzer.typeResolver
                            .resolve(memberName, asMember = true, isNamespace = true)
                    }

                    if (type == ErrorType) return

                    targetScope.resolve(memberName.value).handle(value.range) {
                        if (sym is TypeSymbol)
                            defineUsingModule(sym.staticScope)
                        else
                            defineUsingSymbol(name?.value, sym)

                    }
                }

                else -> target.error(Msg.EXPECTED_MODULE_NAME)
            }
        }
    */

    /*private fun Scope.defineDeclSym(target: DeclStmtNode): Symbol? {
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
    }*/

    private fun ensureDeclared(target: DeclStmtNamedNode): Symbol? {
        target.getResolvedSymbol()?.let { return it }
        analyzer.localDeclPipeline.execute(target)
        return target.getResolvedSymbol()
    }

    private fun resolve(target: ModuleStmtNode) {
        val moduleSym = target.getResolvedSymbol() as? ModuleSymbol
            ?: return

        analyzer.withScopeResolveBody(moduleSym.scope, target.body)
    }

    private fun resolve(target: InterfaceDeclStmtNode) {
        val sym = ensureDeclared(target) as? InterfaceSymbol
            ?: return

        analyzer.withScopeResolveBody(targetScope = sym.scope, body = target.body)
    }

    private fun resolve(target: ClassDeclStmtNode) {
        val sym = ensureDeclared(target) as? ClassSymbol
            ?: return

        analyzer.withScopeResolveBody(targetScope = sym.scope, body = target.body)
    }

    private fun resolve(target: EnumDeclStmtNode) {
        val sym = ensureDeclared(target) as? EnumSymbol
            ?: return

        analyzer.withScopeResolveBody(targetScope = sym.scope, body = target.body)
    }

    private fun resolve(target: VarDeclStmtNode) {
        val sym = ensureDeclared(target) as? VarSymbol ?: return
    }

    private fun resolve(target: FuncDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? FuncSymbol ?: return

        val paramsScope = Scope(parent = scope) // allow sym shadowing in func scope

        val funcScope = FuncScope(
            parent = paramsScope,
            funcSymbol = sym
        )

        val params = sym.params.list

        params.forEach { param ->
            val range = param.range ?: target.range

            paramsScope
                .define(param)
                .handle(range) {}
        }

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