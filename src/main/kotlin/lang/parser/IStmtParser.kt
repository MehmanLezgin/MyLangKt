package lang.parser

import lang.nodes.BaseDatatypeNode
import lang.nodes.BlockNode
import lang.nodes.ExprNode
import lang.nodes.IfElseStmtNode
import lang.nodes.VarDeclStmtNode

interface IStmtParser {
    fun parse(isSingleLine: Boolean = false): ExprNode
    fun parseIfElseStmt(): IfElseStmtNode
    fun analiseParams(exprList: List<ExprNode>): List<VarDeclStmtNode>?
    fun analiseDatatypeList(exprList: List<ExprNode>?): List<BaseDatatypeNode>?
    fun parseBlock(): BlockNode
}