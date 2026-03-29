package lang.semantics.pipeline

import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.resolvers.BaseResolver
import lang.semantics.symbols.*

class NameCollectionPass(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<BlockNode?, Unit>(analyzer = analyzer) {
    private val modResolver = analyzer.modResolver

    override fun resolve(target: BlockNode?) {
        target?.nodes?.forEach { node ->
            when (node) {
                is ModuleStmtNode -> resolve(node)
                is ClassDeclStmtNode -> resolve(node = node)
                is InterfaceDeclStmtNode -> resolve(node = node)
                is EnumDeclStmtNode -> resolve(node = node)
                is VarDeclStmtNode -> resolve(node = node)
                is FuncDeclStmtNode -> resolve(node = node)
                else -> Unit
            }
        }
    }

    private fun resolveBody(sym: TypeSymbol, body: BlockNode?) {
        analyzer.withScope(sym.staticScope) {
            resolve(body)
        }
    }

    fun resolve(node: VarDeclStmtNode) {
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

    fun resolve(node: FuncDeclStmtNode) {
        val modifiers = modResolver.resolveFuncModifiers(node.modifiers)

        withEffectiveScope(isStatic = modifiers.isStatic) {
            scope.defineFuncNameIfNotExist(
                name = node.name.value,
                kind = node.kind,
            )
        }
    }

    fun resolve(node: ModuleStmtNode) {
        val moduleSym = node.getResolvedSymbol() as? ModuleSymbol
            ?: return

        analyzer.withScope(targetScope = moduleSym.scope) {
            resolveBody(moduleSym, node.body)
        }
    }

    fun resolve(node: InterfaceDeclStmtNode) {
        val modifiers = modResolver.resolveInterfaceModifiers(node.modifiers)

        scope.defineInterface(node, modifiers = modifiers)
            .handle(node.name.range) {
                val sym = sym as InterfaceSymbol
                node bind sym

                resolveBody(sym, node.body)
            }
    }

    fun resolve(node: ClassDeclStmtNode) {
        val modifiers = modResolver.resolveClassModifiers(node.modifiers)

        scope.defineClass(node = node, modifiers = modifiers)
            .handle(node.name.range) {
                val sym = sym as ClassSymbol
                node bind sym

                resolveBody(sym, node.body)
            }
    }

    fun resolve(node: EnumDeclStmtNode) {
        val modifiers = modResolver.resolveEnumModifiers(node.modifiers)
        scope.defineEnum(node, modifiers = modifiers)
            .handle(node.name.range) {
                val sym = sym as EnumSymbol
                node bind sym

                resolveBody(sym, node.body)
            }
    }

    /*fun resolve(node: UsingDirectiveNode) {
        val clause = node.clause
        val modifiers = modResolver.resolveUsingModifiers(node.modifiers)

        when (clause) {
            is NameClause.Items -> {
                clause.items.forEach { item ->
                    when (item) {
                        is NameSpecifier.Alias -> {
                            scope.defineAlias(
                                name = item.alias.value,
                                sym = null,
                                visibility = modifiers.visibility
                            ).handle(item.alias.range) {
                                node bind sym
                            }
                        }

                        else -> Unit
                    }
                }
            }

            NameClause.Wildcard -> Unit
        }
    }*/
}