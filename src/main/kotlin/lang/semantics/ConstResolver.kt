package lang.semantics

import lang.nodes.BinOpNode
import lang.nodes.BinOpType
import lang.nodes.ExprNode
import lang.nodes.IdentifierNode
import lang.nodes.LiteralNode
import lang.nodes.UnaryOpNode
import lang.nodes.UnaryOpType
import lang.semantics.scopes.Scope
import lang.semantics.symbols.ConstVarSymbol
import lang.semantics.types.ConstValue
import lang.semantics.types.Type

object ConstResolver {
    private inline fun <reified T> Any.asType(): T? =
        this as? T


    fun resolve(expr: ExprNode?, scope: Scope): ConstValue<*>? {
        if (expr == null) return null
        return when (expr) {
            is LiteralNode<*> -> ConstValue(expr.value)
            is IdentifierNode -> resolve(expr, scope)
            is UnaryOpNode -> resolve(expr, scope)
            is BinOpNode -> resolve(expr, scope)
            else -> null
        }
    }

    fun resolve(expr: IdentifierNode, scope: Scope): ConstValue<*>? {
        val sym = scope.resolve(expr.value)
        if (sym !is ConstVarSymbol<*>) return null
        return sym.value
    }

    fun resolveExprType(expr: ExprNode, scope: Scope): Type? {
        return null
    }

    fun resolve(expr: UnaryOpNode, scope: Scope): ConstValue<*>? {
        val v = resolve(expr.operand, scope) ?: return null

        return when (expr.operator) {
            UnaryOpType.PLUS -> v.unaryPlus()
            UnaryOpType.MINUS -> v.unaryMinus()
            UnaryOpType.NOT -> v.logicalNot()
            UnaryOpType.BITWISE_NOT -> v.bitwiseNot()
            else -> null
        }
    }

    fun resolve(expr: BinOpNode, scope: Scope): ConstValue<*>? {
        val l = resolve(expr.left, scope) ?: return null
        val r = resolve(expr.right, scope) ?: return null

        val result: ConstValue<*>? = when (expr.operator) {
            BinOpType.MUL               -> l * r
            BinOpType.DIV               -> {
                if (r.value is Number && r.value.toDouble() != 0.0)
                    l / r
                else
                    null
            }
            BinOpType.REMAINDER         -> { l % r }
            BinOpType.PLUS              -> {
                if (l.value is String || r.value is String)
                    ConstValue(l.value.toString() + r.value.toString())
                else l + r
            }
            BinOpType.MINUS             -> l - r
            BinOpType.SHIFT_LEFT        -> l shl r
            BinOpType.SHIFT_RIGHT       -> l shr r
            BinOpType.LESS              -> l less r
            BinOpType.LESS_EQUAL        -> l lessEqual r
            BinOpType.GREATER           -> l greater r
            BinOpType.GREATER_EQUAL     -> l greaterEqual r
            BinOpType.EQUAL             -> l equal r
            BinOpType.NOT_EQUAL         -> l notEqual r
            BinOpType.BIN_AND           -> l and r
            BinOpType.BIN_XOR           -> l xor r
            BinOpType.BIN_OR            -> l or r
            BinOpType.AND               -> l logicalAnd r
            BinOpType.OR                -> l logicalOr r
//            BinOpType.CAST -> TODO()
            else -> return null
        }

        return result
    }
}