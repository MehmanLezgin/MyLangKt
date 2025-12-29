package lang.semantics.scopes

import lang.messages.ErrorHandler
import lang.nodes.ConstructorDeclStmtNode
import lang.nodes.DestructorDeclStmtNode
import lang.semantics.symbols.ClassSymbol
import lang.semantics.symbols.Symbol

data class ClassScope(
    override val parent: Scope?,
    override val errorHandler: ErrorHandler
) : Scope(
    parent = parent,
    errorHandler = errorHandler
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