package lang.semantics.scopes

import lang.semantics.symbols.TemplateParamSymbol
import lang.semantics.types.TemplateParam

class TemplateParamScope(
    override val parent: Scope?,
) : Scope(parent = parent) {
    fun defineTemplateParam(tParam: TemplateParam): ScopeResult {
        val sym = TemplateParamSymbol(param = tParam)
        return define(sym)
    }
}