package lang.semantics.scopes

import lang.nodes.ConstructorDeclStmtNode
import lang.nodes.DestructorDeclStmtNode
import lang.semantics.symbols.Symbol

data class ClassScope(
    override val parent: Scope?,
    override val scopeName: String,
) : BaseTypeScope(
    parent = parent,
    scopeName = scopeName,
) {
    fun defineConstructor(node: ConstructorDeclStmtNode) : Symbol? {
        /*val type = scopeType
        if (type is ScopeType.Func) {
            val sym = ConstructorSymbol(
                name = "${type.sym.name}_constr"
            )
        }*/

        return null
    }

    fun defineDestructor(node: DestructorDeclStmtNode) : Symbol? {
        /*val sym = DestructorSymbol(
            name = node.name.value
        )
        return sym*/
        return null
    }
}