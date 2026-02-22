package lang.semantics.pipeline

import lang.messages.Msg
import lang.nodes.BlockNode
import lang.nodes.ExprNode
import lang.nodes.VarDeclStmtNode
import lang.semantics.ISemanticAnalyzer
import lang.semantics.resolvers.BaseResolver
import lang.semantics.symbols.ConstValueSymbol
import lang.semantics.symbols.VarSymbol
import lang.semantics.types.Type
import lang.semantics.types.UnresolvedType

class VarInitPass(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<BlockNode?, Unit>(analyzer = analyzer) {
    override fun resolve(target: BlockNode?) {
        target?.nodes?.forEach { node ->
            when (node) {
                is VarDeclStmtNode -> resolve(target = node)
                else -> Unit
            }
        }
    }

    fun resolve(target: VarDeclStmtNode) {
        val sym = target.getResolvedSymbol() as? VarSymbol ?: return

        var type = sym.type

        if (target.initializer == null && type is UnresolvedType) {
            target.error(Msg.VarMustBeInitialized.format(sym.name))
            return
        }

        resolveInitializer(
            initializer = target.initializer,
            type = type
        ).also {
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

    private fun resolveInitializer(initializer: ExprNode?, type: Type) : Type {
        if (initializer == null) return type

        val resolvedType = analyzer.typeResolver.resolveForType(initializer, type)
            .let { if (type is UnresolvedType) it else type }

        initializer attach resolvedType

        return resolvedType
    }
}