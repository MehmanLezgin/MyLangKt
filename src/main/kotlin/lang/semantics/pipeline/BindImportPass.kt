package lang.semantics.pipeline

import lang.core.SourceRange
import lang.messages.Msg
import lang.messages.Terms.quotes
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.builtin.PrimitivesScope.err
import lang.semantics.builtin.PrimitivesScope.ok
import lang.semantics.resolvers.BaseResolver
import lang.semantics.scopes.ScopeError
import lang.semantics.scopes.ScopeResult
import lang.semantics.scopes.SymbolIMap
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

        import(
            clause = node.clause,
            symbols = scope.symbols,
            range = node.range,
            scopeName = null,
            wildcardAllowed = false
        )
    }

    fun resolve(node: BaseImportStmtNode) {
        if (checkIsVisited(node)) return

        when (node) {
            is ImportModulesStmtNode -> resolveModules(node)
            is ImportFromStmtNode -> resolveFrom(node)
        }
    }

    private fun resolveModulePath(parts: List<IdentifierNode>): ModuleSymbol? {
        var symbols: SymbolIMap = moduleRegPass.allModules
        var module: ModuleSymbol? = null

        for (part in parts) {
            val sym = symbols[part.value]
                ?: return part.error(Msg.MODULE_NOT_DEFINED).let { null }

            if (sym !is ModuleSymbol)
                return part.error(Msg.EXPECTED_MODULE_NAME).let { null }

            module = sym
            symbols = sym.scope.symbols
        }

        return module
    }

    private fun getModule(name: QualifiedName): ModuleSymbol? =
        resolveModulePath(name.parts)


    private fun resolveSymbol(
        name: QualifiedName,
        root: SymbolIMap,
        scopeName: String? = null
    ): ScopeResult? {

        val parts = name.parts
        val last = parts.last()

        val symbols = if (parts.size == 1) {
            root
        } else {
            val module = resolveModulePath(parts.subList(0, parts.lastIndex))
                ?: return null
            module.scope.symbols
        }

        val sym = symbols[last.value]
            ?: return ScopeError.NotDefined(
                symName = last.value,
                scopeName = scopeName?.quotes()
            ).err()

        return sym.ok()
    }

    private fun importAll(symbols: SymbolIMap, range: SourceRange) {
        for (sym in symbols.values)
            scope.define(sym).handle(range) {}
    }

    private fun importItems(items: List<NameSpecifier>, symbols: SymbolIMap, scopeName: String?) {
        for (item in items) {
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

            importOne(
                target = target,
                alias = alias,
                allFrom = allFrom,
                symbols = symbols,
                scopeName = scopeName
            )
        }
    }

    private fun importOne(
        target: QualifiedName,
        alias: IdentifierNode? = null,
        allFrom: Boolean = false,
        symbols: SymbolIMap,
        scopeName: String?
    ) {
        val result = resolveSymbol(target, symbols, scopeName)

        result?.handle(target.range) {
            when {
                allFrom -> {
                    val sym = sym as? TypeSymbol ?: run {
                        semanticError(Msg.EXPECTED_MODULE_NAME, target.range)
                        return@handle
                    }

                    importAll(
                        symbols = sym.staticScope.symbols,
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
                        visibility = Visibility.PRIVATE
                    ).handle(target.range) {}
                }
            }
        }
    }

    private fun import(
        clause: NameClause,
        symbols: SymbolIMap,
        range: SourceRange,
        scopeName: String?,
        wildcardAllowed: Boolean
    ) {
        when (clause) {
            NameClause.Wildcard ->
                if (wildcardAllowed)
                    importAll(symbols, range)
                else
                    semanticError(Msg.WILDCARD_IS_NOT_ALLOWED_HERE, range)

            is NameClause.Items -> importItems(clause.items, symbols, scopeName)
        }
    }

    private fun resolveModules(node: ImportModulesStmtNode) =
        import(
            clause = node.items,
            symbols = moduleRegPass.allModulesAsSymbols,
            range = node.range,
            scopeName = null,
            wildcardAllowed = false
        )

    private fun resolveFrom(node: ImportFromStmtNode) {
        val module = getModule(node.sourceName.target) ?: return

        import(
            clause = node.items,
            symbols = module.scope.symbols,
            range = node.range,
            scopeName = node.sourceName.target.toString(),
            wildcardAllowed = true
        )
    }
}