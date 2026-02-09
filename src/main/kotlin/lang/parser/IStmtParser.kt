package lang.parser

import lang.core.SourceRange
import lang.nodes.BaseDatatypeNode
import lang.nodes.BlockNode
import lang.nodes.ExprNode
import lang.nodes.IdentifierNode
import lang.nodes.IfElseStmtNode
import lang.nodes.ModuleStmtNode
import lang.nodes.VarDeclStmtNode

interface IStmtParser {
    fun parse(isSingleLine: Boolean = false): ExprNode
    fun parseIfElseStmt(): IfElseStmtNode
    fun analiseParams(exprList: List<ExprNode>): List<VarDeclStmtNode>?
    fun analiseDatatypeList(exprList: List<ExprNode>?): List<BaseDatatypeNode>?
    fun parseBlock(): BlockNode
    fun buildModuleHierarchy(list: List<IdentifierNode>, body: BlockNode, range: SourceRange): ModuleStmtNode?
}