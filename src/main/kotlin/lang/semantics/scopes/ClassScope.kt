package lang.semantics.scopes

import lang.semantics.symbols.ClassSymbol

data class ClassScope(
    override val parent: Scope?,
    override val scopeName: String
) : BaseTypeScope(
    parent = parent,
    scopeName = scopeName,
) {
    lateinit var classSym: ClassSymbol
    override fun toString(): String {
        return ""
    }


}