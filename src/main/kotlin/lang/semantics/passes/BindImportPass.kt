package lang.semantics.passes

import lang.messages.Msg
import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.builtin.PrimitivesScope.err
import lang.semantics.builtin.PrimitivesScope.ok
import lang.semantics.resolvers.BaseResolver
import lang.semantics.scopes.Scope
import lang.semantics.scopes.ScopeError
import lang.semantics.scopes.ScopeResult
import lang.semantics.scopes.SymbolIMap
import lang.semantics.scopes.SymbolMap
import lang.semantics.symbols.AliasSymbol
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.Visibility

class BindImportPass(
    override val analyzer: ISemanticAnalyzer,
    private val moduleRegPass: ModuleRegPass
) : BaseResolver<BlockNode?, Unit>(analyzer = analyzer) {
    override fun resolve(target: BlockNode?) {
        target?.nodes?.forEach { node ->
            when (node) {
                is ImportModulesStmtNode -> resolve(node = node)
                is ImportFromStmtNode -> resolve(node = node)
                else -> Unit
            }
        }
    }

    private fun getModule(name: QualifiedName): ModuleSymbol? {
        var currModule: ModuleSymbol? = null
        var targetSymbols: SymbolIMap? = moduleRegPass.allModules

        for (part in name.parts) {
            currModule = targetSymbols
                ?.get(part.value)
                ?.let { sym ->
                    currModule = sym as ModuleSymbol
                    targetSymbols = currModule.scope.symbols
                    currModule
                }

            if (currModule == null) {
                part.error(Msg.MODULE_NOT_DEFINED)
                return null
            }
        }
        /*var targetScope: Scope? = scope

        for (part in name.parts) {
            currModule = targetScope
                ?.resolve(part.value)
                ?.handle(part.range) {
                    if (sym !is ModuleSymbol) {
                        part.error(Msg.EXPECTED_MODULE_NAME)
                        return@handle null
                    }

                    targetScope = currModule?.scope
                    sym
                }
        }*/

        return currModule
    }

    private fun getSymbol(name: QualifiedName, symbols: SymbolIMap): ScopeResult? {
        return if (name.parts.size == 1) {
            val symName = name.parts[0]
            val sym = symbols[symName.value]
                ?: return ScopeError.NotDefined(
                    symName = symName.value,
                    scopeName = null
                ).err()

            sym.ok()
        } else {
            val symName = name.parts.last()
            val moduleName = name.parts.drop(1).toQualifiedName()
            val module = getModule(name = moduleName)
                ?: return null

            val sym = module.scope.symbols[symName.value]
                ?: return ScopeError.NotDefined(
                    symName = symName.value,
                    scopeName = module.scope.absoluteScopePath
                ).err()

            sym.ok()
        }
    }

    private fun importAll(callback: (ScopeResult) -> Unit, symbols: SymbolIMap) {
        symbols.values.forEach { sym ->
            callback(scope.define(sym))
        }
    }

    private fun importItems(items: List<NameSpecifier>, symbols: SymbolIMap, callback: (ScopeResult) -> Unit) {
        items.forEach { item ->
            when (item) {
                is NameSpecifier.Direct -> importOneItem(
                    target = item.target,
                    symbols = symbols,
                    callback = callback
                )

                is NameSpecifier.Alias -> importOneItem(
                    target = item.target,
                    symbols = symbols,
                    alias = item.alias,
                    callback = callback
                )
            }
        }
    }

    private fun importOneItem(
        target: QualifiedName,
        alias: IdentifierNode? = null,
        symbols: SymbolIMap,
        callback: (ScopeResult) -> Unit,
    ) {
        val result = getSymbol(target, symbols)

        result?.handle(target.range) {
            val symToDefine = if (alias == null)
                sym
            else
                AliasSymbol(
                    name = alias.value,
                    visibility = Visibility.PRIVATE,
                    sym = sym
                )

            callback(scope.define(symToDefine))
        }
    }

    private fun import(clause: NameClause, symbols: SymbolIMap, callback: (ScopeResult) -> Unit) {
        when (clause) {
            NameClause.Wildcard -> importAll(callback, symbols)
            is NameClause.Items -> importItems(clause.items, symbols, callback)
        }
    }


    private fun resolve(node: ImportModulesStmtNode) {
        val symbols = moduleRegPass.allModulesAsSymbols

        import(clause = node.items, symbols = symbols) {
            it.handle(node.range) {}
        }
    }

    private fun resolve(node: ImportFromStmtNode) {
        val module = getModule(node.sourceName.target)
            ?: return

        import(clause = node.items, symbols = module.scope.symbols) {
            it.handle(node.range) {}
        }
    }

}