package lang.semantics.resolvers

import lang.messages.Msg
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.scopes.ClassScope
import lang.semantics.scopes.FuncParamsScope
import lang.semantics.scopes.FuncScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.*
import lang.semantics.types.LazyType
import lang.semantics.types.UnresolvedType
import lang.semantics.types.isNullOrUnresolved
import lang.semantics.types.lazyType

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
            is TemplateStmtNode -> resolve(target)
        }
    }

    private fun resolve(target: UsingDirectiveNode) {
        analyzer.localDeclPipeline.execute(target)
    }

    private fun ensureDeclared(target: StmtNode): Symbol? {
        target.getResolvedSymbol()?.let { return it }
        analyzer.localDeclPipeline.execute(target)
        return target.getResolvedSymbol()
    }

    private fun resolve(target: TemplateStmtNode) {
        ensureDeclared(target)
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

        withEffectiveScope(isStatic = sym.modifiers.isStatic) {
            val paramsScope = FuncParamsScope(parent = scope) // allow sym shadowing in func scope

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

            val body = target.body ?: return@withEffectiveScope

            fun resolveBody() =
                analyzer.withScopeResolveBody(targetScope = funcScope, body = body)

            if (sym.returnType !is UnresolvedType)
                resolveBody()
            else
                sym.returnType = lazyType {
                    resolveBody()
                    val bodyType = body.getResolvedType()
                    if (bodyType.isNullOrUnresolved())
                        body.error(Msg.CannotResolveFuncType.format(name = sym.name))

                    bodyType ?: UnresolvedType
                }
        }
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