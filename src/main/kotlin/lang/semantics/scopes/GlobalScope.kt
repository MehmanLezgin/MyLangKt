package lang.semantics.scopes

import lang.nodes.ClassDeclStmtNode
import lang.nodes.EnumDeclStmtNode
import lang.nodes.InterfaceDeclStmtNode
import lang.semantics.symbols.ClassSymbol
import lang.semantics.symbols.EnumSymbol
import lang.semantics.symbols.InterfaceSymbol
import lang.semantics.symbols.Symbol

class GlobalScope : Scope(parent = null) {
    fun defineInterface(node: InterfaceDeclStmtNode) : Symbol? {
        val sym = InterfaceSymbol(
            name = node.name.value
        )
        return sym
    }

    fun defineClass(node: ClassDeclStmtNode) : Symbol? {
//        val sym = ClassSymbol(
//            name = node.name.value,
//            scope = ClassScope(parent = this, )
//        )
//        return sym
        return null
    }

    fun defineEnum(node: EnumDeclStmtNode) : Symbol? {
        val sym = EnumSymbol(
            name = node.name?.value ?: return null
        )

        return sym
    }
}