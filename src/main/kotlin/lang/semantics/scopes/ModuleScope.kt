package lang.semantics.scopes

import lang.semantics.builtin.PrimitivesScope

class ModuleScope(
) : NamespaceScope(
    parent = PrimitivesScope,
    scopeName = "",
    isExport = true
)