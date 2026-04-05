package lang.semantics.symbols

import lang.nodes.FuncDeclStmtNode
import lang.semantics.scopes.Scope
import lang.semantics.types.TemplateParam
import lang.semantics.types.Type

data class TemplateFuncSymbol(
    override val name: String,
    override val ast: FuncDeclStmtNode,
    override val templateParams: List<TemplateParam>,
    override val params: FuncParamListSymbol,
    override val returnType: Type,
    override val modifiers: Modifiers,
) : TemplateSymbol(
    name = name,
    ast = ast,
    templateParams = templateParams,
    modifiers = modifiers
), CallableSymbol {
    override fun toOverloadedFuncSymbol(accessScope: Scope) =
        OverloadedFuncSymbol(
            name = name,
            kind = FuncKind.FUNCTION,
            candidates = mutableListOf(this),
            accessScope = accessScope
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as TemplateFuncSymbol

        if (name != other.name) return false
        if (templateParams != other.templateParams) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + templateParams.hashCode()
        return result
    }


}