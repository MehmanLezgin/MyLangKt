package lang.semantics.pipeline

import lang.messages.Msg
import lang.nodes.BlockNode
import lang.nodes.ClassDeclStmtNode
import lang.nodes.DeclStmtNamedNode
import lang.nodes.EnumDeclStmtNode
import lang.nodes.ExprNode
import lang.nodes.FuncDeclStmtNode
import lang.nodes.InterfaceDeclStmtNode
import lang.nodes.ModuleStmtNode
import lang.nodes.VarDeclStmtNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.resolvers.BaseResolver
import lang.semantics.scopes.Scope
import lang.semantics.symbols.ClassSymbol
import lang.semantics.symbols.ConstValueSymbol
import lang.semantics.symbols.EnumSymbol
import lang.semantics.symbols.FuncSymbol
import lang.semantics.symbols.InterfaceSymbol
import lang.semantics.symbols.ModuleSymbol
import lang.semantics.symbols.VarSymbol
import lang.semantics.types.Type
import lang.semantics.types.UnresolvedType
import lang.semantics.types.lazyType

class VarInitPass(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<BlockNode?, Unit>(analyzer = analyzer) {
    override fun resolve(target: BlockNode?) {
        target?.nodes?.forEach { node ->
            when (node) {
                is VarDeclStmtNode -> resolve(target = node)
                is EnumDeclStmtNode -> resolve(target = node)
                is ModuleStmtNode -> resolve(target = node)
                is ClassDeclStmtNode -> resolve(target = node)
                is InterfaceDeclStmtNode -> resolve(target = node)
                else -> Unit
            }
        }
    }

    fun resolve(target: EnumDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? EnumSymbol ?: return
        withScopeResolve(sym.scope, target.body)
    }

    fun resolve(target: ModuleStmtNode) {
        val sym = target.getResolvedSymbol() as? ModuleSymbol ?: return
        withScopeResolve(sym.scope, target.body)
    }

    fun resolve(target: ClassDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? ClassSymbol ?: return
        withScopeResolve(sym.scope, target.body)
    }

    fun resolve(target: InterfaceDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? InterfaceSymbol ?: return
        withScopeResolve(sym.scope, target.body)
    }

    private fun withScopeResolve(targetScope: Scope, body: BlockNode?) {
        analyzer.withScope(targetScope) {
            resolve(body)
        }
    }

    fun resolve(target: VarDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? VarSymbol ?: return

        var type = sym.type

        if (target.initializer == null && type is UnresolvedType) {
            target.error(Msg.VarMustBeInitialized.format(sym.name))
            return
        }

        analyzer.withScope(scope) {
            resolveInitializer(
                initializer = target.initializer,
                type = type
            )
        }.also {
            sym.type = it
            type = it
        }

        val isConst = sym.type.isConst

        if (isConst && !sym.modifiers.isStatic && scope.isTypeScope())
            target.error(Msg.CONST_VAR_MUST_BE_STATIC)

        val initSym = target.initializer?.getResolvedSymbol()

        if (initSym is ConstValueSymbol) {
            var value = initSym.value

            if (value != null && value.type != type)
                value = analyzer.constResolver.resolveCast(value, type)

            sym.constValue = value
        }
    }

    private fun resolveInitializer(initializer: ExprNode?, type: Type): Type {
        if (initializer == null) return type

        val resolvedType =
            analyzer.typeResolver.resolveForType(initializer, type)
                .also {
                    if (type is UnresolvedType) it else type
                    initializer attach it
                }


        return resolvedType
    }
}