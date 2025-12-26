package lang.nodes

/*
sealed class BinOpNode(
    open val left: ExprNode,
    open val right: ExprNode,
    val operator: BinOpType,
    override val pos: SymbolPos
) : ExprNode(pos) {
    data class Arrow(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Dot(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Mul(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Div(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Remainder(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Plus(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Minus(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Shift_left(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Shift_right(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Less(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Less_equal(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Greater(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Greater_equal(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Equal(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Not_equal(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Amp(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Xor(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class BinOr(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class And(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Or(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Assign(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class PlusAssign(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class MinusAssign(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class MulAssign(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class DivAssign(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class RemainderAssign(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class AndAssign(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class OrAssign(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class XorAssign(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class ShiftLeftAssign(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class ShiftRightAssign(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

    data class Cast(
        override val left: ExprNode,
        override val right: ExprNode,
        override val pos: SymbolPos
    ) :
        BinOpNode(left, right, pos)

}*/
