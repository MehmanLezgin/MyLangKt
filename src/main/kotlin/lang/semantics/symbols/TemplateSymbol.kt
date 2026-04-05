package lang.semantics.symbols

import lang.nodes.DeclStmtNamedNode
import lang.semantics.types.TemplateParam

open class TemplateSymbol(
    override val name: String,
    open val ast: DeclStmtNamedNode,
    open val templateParams: List<TemplateParam>,
    override val modifiers: Modifiers = Modifiers()
) : Symbol {
    fun deepCopyAst() = ast.mapRecursive { it }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemplateSymbol

        if (name != other.name) return false
        if (templateParams != other.templateParams) return false
        if (modifiers != other.modifiers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + templateParams.hashCode()
        result = 31 * result + modifiers.hashCode()
        return result
    }


}
