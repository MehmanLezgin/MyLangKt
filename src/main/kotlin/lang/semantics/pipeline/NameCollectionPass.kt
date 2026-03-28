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
                is ClassDeclStmtNode -> resolve(target = node)
                is InterfaceDeclStmtNode -> resolve(target = node)
                is EnumDeclStmtNode -> resolve(target = node)
                is UsingDirectiveNode -> resolve(target = node)
                is VarDeclStmtNode -> resolve(target = node)
                is FuncDeclStmtNode -> resolve(target = node)
                else -> Unit
            }
        }
    }

    private fun resolveBody(sym: TypeSymbol, body: BlockNode?) {
        analyzer.withScope(sym.staticScope) {
            resolve(body)
        }
    }

    fun resolve(target: VarDeclStmtNode) {
        val modifiers = modResolver.resolveVarModifiers(target.modifiers)

        withEffectiveScope(isStatic = modifiers.isStatic) {
            scope.defineVarName(
                name = target.name.value,
                isMutable = target.isMutable,
                modifiers = modifiers
            ).handle(target.name.range) {
                val sym = sym as VarSymbol
                target bind sym
            }
        }
    }

    fun resolve(target: FuncDeclStmtNode) {
        val modifiers = modResolver.resolveFuncModifiers(target.modifiers)

        withEffectiveScope(isStatic = modifiers.isStatic) {
            scope.defineFuncNameIfNotExist(
                name = target.name.value,
                kind = target.kind
            )
        }
    }

    fun resolve(target: ModuleStmtNode) {
        val moduleSym = target.getResolvedSymbol() as? ModuleSymbol
            ?: return

        analyzer.withScope(targetScope = moduleSym.scope) {
            resolveBody(moduleSym, target.body)
        }
    }

    fun resolve(target: InterfaceDeclStmtNode) {
        val modifiers = modResolver.resolveInterfaceModifiers(target.modifiers)

        scope.defineInterface(target, modifiers = modifiers)
            .handle(target.name.range) {
                val sym = sym as InterfaceSymbol
                target bind sym

                resolveBody(sym, target.body)
            }
    }

    fun resolve(target: ClassDeclStmtNode) {
        val modifiers = modResolver.resolveClassModifiers(target.modifiers)

        scope.defineClass(node = target, modifiers = modifiers)
            .handle(target.name.range) {
                val sym = sym as ClassSymbol
                target bind sym

                resolveBody(sym, target.body)
            }
    }

    fun resolve(target: EnumDeclStmtNode) {
        val modifiers = modResolver.resolveEnumModifiers(target.modifiers)
        scope.defineEnum(target, modifiers = modifiers)
            .handle(target.name.range) {
                val sym = sym as EnumSymbol
                target bind sym

                resolveBody(sym, target.body)
            }
    }

    fun resolve(target: UsingDirectiveNode) {
        val clause = target.clause
        val modifiers = modResolver.resolveUsingModifiers(target.modifiers)

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
                                target bind sym
                            }
                        }

                        else -> Unit
                    }
                }
            }

            NameClause.Wildcard -> Unit
        }
    }
}