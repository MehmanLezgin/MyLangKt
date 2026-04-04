package lang.parser

import lang.infrastructure.SourceRange
import lang.nodes.BlockNode
import lang.nodes.ExprNode
import lang.nodes.IdentifierNode
import lang.nodes.IfElseStmtNode
import lang.nodes.ModuleStmtNode

interface IStmtParser {
    fun parse(isSingleLine: Boolean = false): ExprNode
    fun parseIfElseStmt(): IfElseStmtNode
    fun parseBlock(): BlockNode
    fun parseMultilineBlock(): BlockNode
    fun buildModuleHierarchy(list: List<IdentifierNode>, body: BlockNode, range: SourceRange): ModuleStmtNode?
}