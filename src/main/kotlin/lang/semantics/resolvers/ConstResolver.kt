package lang.semantics.resolvers

import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.symbols.ConstValueSymbol
import lang.semantics.types.ConstValue
import lang.semantics.types.Type

class ConstResolver(
    override val analyzer: ISemanticAnalyzer
) : BaseResolver<ExprNode?, ConstValue<*>?>(analyzer = analyzer) {
    override fun resolve(target: ExprNode?): ConstValue<*>? {
        if (target == null) return null
        return when (target) {
            is LiteralNode<*> -> ConstValue.from(target)
            is IdentifierNode -> resolve(target)
            is ScopedDatatypeNode -> resolve(target)
            is UnaryOpNode -> resolve(target)
            is BinOpNode -> resolve(target)
            is DatatypeNode -> resolve(target)
            else -> null
        }
    }

    private fun resolve(expr: ScopedDatatypeNode): ConstValue<*>? {
        val type = analyzer.typeResolver.resolve(expr.base, isNamespace = true)
        val scope = type.declaration?.staticScope
        val name = expr.member.identifier.value
        val sym = scope?.resolve(name = name, asMember = true)
        if (sym !is ConstValueSymbol) return null
        return sym.value
    }

    private fun resolve(expr: IdentifierNode): ConstValue<*>? {
        val sym = analyzer.scope.resolve(expr.value)
        if (sym !is ConstValueSymbol) return null
        return sym.value
    }

    private fun resolve(expr: UnaryOpNode): ConstValue<*>? {
        val v = resolve(expr.operand) ?: return null

        return when (expr.operator) {
            UnaryOpType.PLUS -> v.unaryPlus()
            UnaryOpType.MINUS -> v.unaryMinus()
            UnaryOpType.NOT -> v.logicalNot()
            UnaryOpType.BITWISE_NOT -> v.bitwiseNot()
            else -> null
        }
    }

    fun resolveCast(
        constValue: ConstValue<*>, type: Type
    ): ConstValue<*>? {
        if (!type.isConst || type.isExprType) return null
        val value = constValue.toNumber()
        val casted = ConstValue.convertToType(type, value) ?: return null
        return ConstValue(casted)
    }

    private fun resolveCast(expr: BinOpNode): ConstValue<*>? {
        val left = resolve(expr.left) ?: return null
        val type = analyzer.typeResolver.resolve(expr.right)
        return resolveCast(left, type)
    }

    private fun resolve(expr: BinOpNode): ConstValue<*>? {
        if (expr.operator == BinOpType.CAST)
            return resolveCast(expr)

        val l = resolve(expr.left) ?: return null
        val r = resolve(expr.right) ?: return null

        try {
            val result: ConstValue<*>? = when (expr.operator) {
                BinOpType.MUL -> l * r
                BinOpType.DIV -> {
                    if (r.value is Number && r.value.toDouble() != 0.0)
                        l / r
                    else
                        null
                }

                BinOpType.REMAINDER -> {
                    l % r
                }

                BinOpType.PLUS -> {
                    if (l.value is String || r.value is String)
                        ConstValue(l.value.toString() + r.value.toString())
                    else l + r
                }

                BinOpType.MINUS -> l - r
                BinOpType.SHIFT_LEFT -> l shl r
                BinOpType.SHIFT_RIGHT -> l shr r
                BinOpType.LESS -> l less r
                BinOpType.LESS_EQUAL -> l lessEqual r
                BinOpType.GREATER -> l greater r
                BinOpType.GREATER_EQUAL -> l greaterEqual r
                BinOpType.EQUAL -> l equal r
                BinOpType.NOT_EQUAL -> l notEqual r
                BinOpType.BIN_AND -> l and r
                BinOpType.BIN_XOR -> l xor r
                BinOpType.BIN_OR -> l or r
                BinOpType.AND -> l logicalAnd r
                BinOpType.OR -> l logicalOr r
                else -> return null
            }

            return result
        } catch (_: Exception) {
            return null
        }
    }
}