package lang.semantics.pipeline

import lang.messages.Msg
import lang.nodes.BlockNode
import lang.nodes.ClassDeclStmtNode
import lang.nodes.EnumDeclStmtNode
import lang.nodes.FuncDeclStmtNode
import lang.nodes.InterfaceDeclStmtNode
import lang.nodes.ModuleStmtNode
import lang.nodes.OperNode
import lang.nodes.UsingDirectiveNode
import lang.nodes.VarDeclStmtNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.resolvers.BaseResolver
import lang.semantics.symbols.ClassSymbol
import lang.semantics.symbols.EnumSymbol
import lang.semantics.symbols.InterfaceSymbol
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.OverloadedFuncSymbol
import lang.semantics.symbols.TypeSymbol
import lang.semantics.symbols.VarSymbol

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

        scope.defineVarName(
            name = target.name.value,
            isMutable = target.isMutable,
            modifiers = modifiers
        ).handle(target.name.range) {
            val sym = sym as VarSymbol
            target bind sym
        }
    }

    fun resolve(target: FuncDeclStmtNode) {
        scope.defineFuncNameIfNotExist(
            name = target.name.value,
            isOperator = target.name is OperNode
        )
    }

    fun resolve(target: ModuleStmtNode) {
        val modifiers = analyzer.modResolver.resolveModuleModifiers(target.modifiers)

        val moduleSym = target.getResolvedSymbol() as? ModuleSymbol

        if (moduleSym == null) {
            target.error(Msg.SymbolIsNotRegistered.format(target.name.value))
            return
        }

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
        scope.defineClass(target, modifiers = modifiers)
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
        if (!target.isType) return
        val name = target.name ?: return
        val modifiers = modResolver.resolveUsingModifiers(target.modifiers)
        scope.defineUsing(name = name.value, visibility = modifiers.visibility)
            .handle(target.name.range) {
                target bind sym
            }
    }
}