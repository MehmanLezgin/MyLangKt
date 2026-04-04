package lang.semantics.pipeline

import lang.infrastructure.SourceRange
import lang.messages.Msg
import lang.messages.Terms
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.core.PrimitivesScope.err
import lang.core.PrimitivesScope.ok
import lang.semantics.resolvers.BaseResolver
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeError
import lang.semantics.scopes.ScopeResult
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.TypeSymbol
import lang.semantics.symbols.Visibility

class BindImportPass(
    override val analyzer: ISemanticAnalyzer,
    private val moduleRegPass: ModuleRegPass
) : BaseResolver<BlockNode?, Unit>(analyzer) {
    private val visitedNodes = mutableMapOf<StmtNode, Boolean>()

    override fun resolve(target: BlockNode?) {
        target ?: return
        for (node in target.nodes) {
            when (node) {
                is BaseImportStmtNode -> resolve(node)
                else -> Unit
            }
        }
    }

    private fun checkIsVisited(node: StmtNode): Boolean {
        if (visitedNodes[node] == true) return true
        visitedNodes[node] = true
        return false
    }

    fun resolve(node: UsingDirectiveNode) {
        if (checkIsVisited(node)) return
        val modifiers = analyzer.modResolver.resolve(target = node.modifiers)

        val clause = node.clause

        if (clause !is NameClause.Items) {
            semanticError(Msg.WILDCARD_IS_NOT_ALLOWED_HERE, node.range)
            return
        }

        import(
            clause = clause,
            targetScope = moduleRegPass.modulesContainer,
            range = node.range,
            wildcardAllowed = false,
            visibility = modifiers.visibility
        )
    }

    fun resolve(node: BaseImportStmtNode) {
        if (checkIsVisited(node)) return

        when (node) {
            is ImportModulesStmtNode -> resolveImportModules(node)
            is ImportFromStmtNode -> resolveFrom(node)
        }
    }

    private fun resolveModulePath(parts: List<IdentifierNode>): ScopeResult {
        var module: ModuleSymbol? = null

        var currScope = moduleRegPass.modulesContainer

        var name = ""

        for (part in parts) {
            name = part.value

            val result = currScope.resolveModule(
                name = name,
                fromScope = scope
            )

            if (result is ScopeResult.Success<*>) {
                val sym = result.sym as ModuleSymbol
                module = sym
                currScope = sym.scope
            }

            continue
        }

        if (module == null) {
            return ScopeError.NotDefined(
                itemKind = Terms.MODULE,
                symName = name,
                scopeName = currScope.absoluteScopePath
            ).err()
        }

        return module.ok()
    }

    private fun getModule(name: QualifiedName): ScopeResult =
        resolveModulePath(name.parts)

    private fun resolveSymbol(
        name: QualifiedName,
        targetScope: Scope,
    ): ScopeResult? {

        val parts = name.parts
        val last = parts.last()

        val finalScope = if (parts.size == 1) {
            targetScope
        } else {
            val module = resolveModulePath(parts.subList(0, parts.lastIndex))
                .handle(name.range) {
                    sym as ModuleSymbol
                } ?: return null

            module.scope
        }

        return finalScope.resolve(
            name = last.value,
            from = scope,
            asMember = true
        )
    }

    private fun importAll(targetScope: Scope, range: SourceRange) {
        val symbols = targetScope.symbols

        for (sym in symbols.values) {
            val isAccessible = targetScope.isSymAccessibleFrom(
                sym = sym,
                from = scope,
                asMember = true
            )

            if (!isAccessible) continue

            scope.define(sym).handle(range) {}
        }
    }

    private fun importItem(
        item: NameSpecifier,
        targetScope: Scope,
        visibility: Visibility
    ) {
        val target: QualifiedName = item.target
        var alias: IdentifierNode? = null
        var allFrom = false

        when (item) {
            is NameSpecifier.Direct -> Unit

            is NameSpecifier.Alias -> {
                alias = item.alias
            }

            is NameSpecifier.AllFrom -> {
                alias = null
                allFrom = true
            }
        }

        return importItem(
            target = target,
            alias = alias,
            allFrom = allFrom,
            targetScope = targetScope,
            visibility = visibility
        )
    }

    private fun importItems(
        items: List<NameSpecifier>,
        targetScope: Scope,
        visibility: Visibility
    ) {
        for (item in items) {
            importItem(
                item = item,
                targetScope = targetScope,
                visibility = visibility
            )
        }
    }

    private fun importItem(
        target: QualifiedName,
        alias: IdentifierNode? = null,
        allFrom: Boolean = false,
        targetScope: Scope,
        visibility: Visibility
    ) {
        val result = resolveSymbol(target, targetScope)

        result?.handle(target.range) {
            when {
                allFrom -> {
                    val sym = sym as? TypeSymbol ?: run {
                        semanticError(Msg.EXPECTED_MODULE_NAME, target.range)
                        return@handle
                    }

                    importAll(
                        targetScope = sym.staticScope,
                        range = target.range ?: return@handle
                    )
                }

                alias == null -> {
                    scope.define(sym).handle(target.range) {}
                }

                else -> {
                    scope.defineAlias(
                        name = alias.value,
                        sym = sym,
                        visibility = visibility
                    ).handle(target.range) {}
                }
            }
        }
    }

    private fun import(
        clause: NameClause,
        targetScope: Scope,
        range: SourceRange,
        wildcardAllowed: Boolean,
        visibility: Visibility
    ) {
        when (clause) {
            NameClause.Wildcard ->
                if (wildcardAllowed)
                    importAll(
                        targetScope = targetScope,
                        range = range
                    )
                else
                    semanticError(Msg.WILDCARD_IS_NOT_ALLOWED_HERE, range)

            is NameClause.Items -> importItems(
                items = clause.items,
                targetScope = targetScope,
                visibility = visibility
            )
        }
    }

    private fun resolveImportModules(node: ImportModulesStmtNode) =
        import(
            clause = node.items,
            targetScope = moduleRegPass.modulesContainer,
            range = node.range,
            wildcardAllowed = false,
            visibility = Visibility.PRIVATE
        )

    private fun resolveFrom(node: ImportFromStmtNode) {
        val module = getModule(node.sourceName.target)
            .handle(node.range) {
                sym as ModuleSymbol
            } ?: return

        import(
            clause = node.items,
            targetScope = module.scope,
            range = node.range,
            wildcardAllowed = true,
            visibility = Visibility.PRIVATE
        )
    }
}