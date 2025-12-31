package lang.semantics.resolvers

import lang.nodes.*
import lang.semantics.ISemanticAnalyzer
import lang.semantics.symbols.ConstVarSymbol
import lang.semantics.types.ConstValue

class ConstResolver(
    override val ctx: ISemanticAnalyzer
) : BaseResolver<ExprNode?, ConstValue<*>?>(ctx = ctx) {
    override fun resolve(target: ExprNode?): ConstValue<*>? {
        if (target == null) return null
        return when (target) {
            is LiteralNode<*> -> ConstValue(target.value)
            is IdentifierNode -> resolve(target)
            is UnaryOpNode -> resolve(target)
            is BinOpNode -> resolve(target)
            else -> null
        }
    }

    private fun resolve(expr: IdentifierNode): ConstValue<*>? {
        val sym = ctx.scope.resolve(expr.value)
        if (sym !is ConstVarSymbol) return null
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

    private fun resolve(expr: BinOpNode): ConstValue<*>? {
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
//            BinOpType.CAST -> TODO()
                else -> return null
            }

            return result
        } catch (_: Exception) {
            return null
        }
    }
}