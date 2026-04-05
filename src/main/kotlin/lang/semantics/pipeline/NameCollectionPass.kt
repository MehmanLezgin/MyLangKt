package lang.semantics.pipeline

import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.resolvers.BaseResolver
import lang.semantics.scopes.ScopeResult
import lang.semantics.scopes.TemplateParamScope
import lang.semantics.symbols.*

class NameCollectionPass(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<BlockNode?, Unit>(analyzer = analyzer) {
    private val modResolver = analyzer.modResolver
    val visited = mutableMapOf<ExprNode, Boolean>()

    override fun resolve(target: BlockNode?) {
        target?.nodes?.forEach { node ->
            when (node) {
                is ModuleStmtNode -> resolve(node)
                is ClassDeclStmtNode -> resolve(node = node)
                is InterfaceDeclStmtNode -> resolve(node = node)
                is EnumDeclStmtNode -> resolve(node = node)
                is VarDeclStmtNode -> resolve(node = node)
                is FuncDeclStmtNode -> resolve(node = node)
                is TemplateStmtNode -> resolve(node = node)
                else -> Unit
            }
        }
    }

    private fun checkVisited(node: ExprNode): Boolean {
        if (visited[node] == true) return true
        visited[node] = true
        return false
    }


    private fun resolveBody(sym: TypeSymbol, body: BlockNode?) {
        analyzer.withScope(sym.staticScope) {
            resolve(body)
        }
    }

    fun resolve(node: TemplateStmtNode) {
        if (checkVisited(node)) return

        val modifiers = modResolver.resolveTemplateModifiers(node)
        val templateParams = analyzer.typeResolver.resolveTemplateParams(params = node.params)

        fun defineNormalTemplate() =
            scope.defineTemplate(
                name = node.name.value,
                modifiers = modifiers,
                ast = node.declStmt,
                templateParams = templateParams,
            )

        fun defineFuncTemplate(ast: FuncDeclStmtNode): ScopeResult {
            val templateParamScope = TemplateParamScope(parent = scope)


            val (params, returnType) = analyzer.withScope(templateParamScope) {
                templateParams.forEach { tParam ->
                    templateParamScope.defineTemplateParam(tParam)
                }

                val params = analyzer.typeResolver.resolveFuncParams(params = ast.params)
                val returnType = analyzer.typeResolver.resolve(target = ast.returnType)

                params to returnType
            }

            return scope.defineFuncTemplate(
                name = node.name.value,
                modifiers = modifiers,
                ast = ast,
                templateParams = templateParams,
                params = params,
                returnType = returnType,
            )

        }

        withEffectiveScope(isStatic = modifiers.isStatic) {
            when (val ast = node.declStmt) {
                is FuncDeclStmtNode -> defineFuncTemplate(ast)
                else -> defineNormalTemplate()
            }.handle(node.name.range) {
                node bind sym
            }
        }
    }

    fun resolve(node: VarDeclStmtNode) {
        if (checkVisited(node)) return

        val modifiers = modResolver.resolveVarModifiers(node.modifiers)

        withEffectiveScope(isStatic = modifiers.isStatic) {
            scope.defineVarName(
                name = node.name.value,
                isMutable = node.isMutable,
                modifiers = modifiers
            ).handle(node.name.range) {
                val sym = sym as VarSymbol
                node bind sym
            }
        }
    }

    private fun FuncDeclStmtNode.resolveFuncModifiers() = when (this) {
        is ConstructorDeclStmtNode ->
            modResolver.resolveConstructorModifiers(this.modifiers)

        is DestructorDeclStmtNode ->
            modResolver.resolveDestructorModifiers(this.modifiers)

        else ->
            modResolver.resolveFuncModifiers(this.modifiers)
    }


    fun resolve(node: FuncDeclStmtNode) {
        if (checkVisited(node)) return
        val modifiers = node.resolveFuncModifiers()

        withEffectiveScope(isStatic = modifiers.isStatic) {
            scope.defineFuncNameIfNotExist(
                name = node.name.value,
                kind = node.kind,
            )
        }
    }

    fun resolve(node: ModuleStmtNode) {
        if (checkVisited(node)) return

        val moduleSym = node.getResolvedSymbol() as? ModuleSymbol
            ?: return

        analyzer.withScope(targetScope = moduleSym.scope) {
            resolveBody(moduleSym, node.body)
        }
    }

    fun resolve(node: InterfaceDeclStmtNode) {
        if (checkVisited(node)) return

        val modifiers = modResolver.resolveInterfaceModifiers(node.modifiers)

        scope.defineInterface(node, modifiers = modifiers)
            .handle(node.name.range) {
                val sym = sym as InterfaceSymbol
                node bind sym

                resolveBody(sym, node.body)
            }
    }

    fun resolve(node: ClassDeclStmtNode) {
        if (checkVisited(node)) return

        val modifiers = modResolver.resolveClassModifiers(node.modifiers)

        scope.defineClass(node = node, modifiers = modifiers)
            .handle(node.name.range) {
                val sym = sym as ClassSymbol
                node bind sym

                resolveBody(sym, node.body)
            }
    }

    fun resolve(node: EnumDeclStmtNode) {
        if (checkVisited(node)) return

        val modifiers = modResolver.resolveEnumModifiers(node.modifiers)
        scope.defineEnum(node, modifiers = modifiers)
            .handle(node.name.range) {
                val sym = sym as EnumSymbol
                node bind sym

                resolveBody(sym, node.body)
            }
    }
}