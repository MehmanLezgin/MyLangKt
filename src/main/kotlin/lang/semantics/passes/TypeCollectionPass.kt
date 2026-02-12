package lang.semantics.passes

import lang.messages.Msg
import lang.nodes.BlockNode
import lang.nodes.ClassDeclStmtNode
import lang.nodes.EnumDeclStmtNode
import lang.nodes.InterfaceDeclStmtNode
import lang.nodes.ModuleStmtNode
import lang.nodes.UsingDirectiveNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.resolvers.BaseResolver
import lang.semantics.symbols.ClassSymbol
import lang.semantics.symbols.EnumSymbol
import lang.semantics.symbols.InterfaceSymbol
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.TypeSymbol

class TypeCollectionPass(
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
                else -> Unit
            }
        }
    }

    private fun resolveBody(sym: TypeSymbol, body: BlockNode?) {
        analyzer.withScope(sym.staticScope) {
            resolve(body)
        }
    }

    private fun resolve(target: ModuleStmtNode) {
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

    private fun resolve(target: InterfaceDeclStmtNode) {
        val modifiers = modResolver.resolveInterfaceModifiers(target.modifiers)
        
        scope.defineInterface(target, modifiers = modifiers)
            .handle(target.name.range) {
                val sym = sym as InterfaceSymbol
                target bind sym

                resolveBody(sym, target.body)
            }
    }

    private fun resolve(target: ClassDeclStmtNode) {
        val modifiers = modResolver.resolveClassModifiers(target.modifiers)
        scope.defineClass(target, modifiers = modifiers)
            .handle(target.name.range) {
                val sym = sym as ClassSymbol
                target bind sym

                resolveBody(sym, target.body)
            }
    }

    private fun resolve(target: EnumDeclStmtNode) {
        val modifiers = modResolver.resolveEnumModifiers(target.modifiers)
        scope.defineEnum(target, modifiers = modifiers)
            .handle(target.name.range) {
                val sym = sym as EnumSymbol
                target bind sym

                resolveBody(sym, target.body)
            }
    }

    private fun resolve(target: UsingDirectiveNode) {
        if (!target.isType) return
        val name = target.name ?: return
        val modifiers = modResolver.resolveUsingModifiers(target.modifiers)
        scope.defineUsing(name = name.value, visibility = modifiers.visibility)
            .handle(target.name.range) {
                target bind sym
            }
    }
}