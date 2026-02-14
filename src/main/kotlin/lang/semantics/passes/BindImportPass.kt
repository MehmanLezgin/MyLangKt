package lang.semantics.passes

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
import lang.semantics.symbols.AliasSymbol
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.Visibility

class BindImportPass(
    override val analyzer: ISemanticAnalyzer,
    private val moduleRegPass: ModuleRegPass
) : BaseResolver<BlockNode?, Unit>(analyzer) {

    override fun resolve(target: BlockNode?) {
        target ?: return
        for (node in target.nodes) {
            when (node) {
                is ImportModulesStmtNode -> resolveModules(node)
                is ImportFromStmtNode -> resolveFrom(node)
                else -> Unit
            }
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
            val target: QualifiedName
            val alias: IdentifierNode?

            when (item) {
                is NameSpecifier.Direct -> {
                    target = item.target
                    alias = null
                }

                is NameSpecifier.Alias -> {
                    target = item.target
                    alias = item.alias
                }
            }

            importOne(target, alias, symbols, scopeName)
        }
    }

    private fun importOne(
        target: QualifiedName,
        alias: IdentifierNode? = null,
        symbols: SymbolIMap,
        scopeName: String?
    ) {
        val result = resolveSymbol(target, symbols, scopeName)

        result?.handle(target.range) {
            val symToDefine = if (alias == null)
                sym
            else
                AliasSymbol(
                    name = alias.value,
                    visibility = Visibility.PRIVATE,
                    sym = sym
                )

            scope.define(symToDefine).handle(target.range) {}
        }
    }

    private fun import(
        clause: NameClause,
        symbols: SymbolIMap,
        range: SourceRange,
        scopeName: String?
    ) {
        when (clause) {
            NameClause.Wildcard -> importAll(symbols, range)
            is NameClause.Items -> importItems(clause.items, symbols, scopeName)
        }
    }

    private fun resolveModules(node: ImportModulesStmtNode) =
        import(
            clause = node.items,
            symbols = moduleRegPass.allModulesAsSymbols,
            range = node.range,
            scopeName = null
        )

    private fun resolveFrom(node: ImportFromStmtNode) {
        val module = getModule(node.sourceName.target) ?: return

        import(
            clause = node.items,
            symbols = module.scope.symbols,
            range = node.range,
            scopeName = node.sourceName.target.toString()
        )
    }
}