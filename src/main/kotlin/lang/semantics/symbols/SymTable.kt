package lang.semantics.symbols

/*
class SymTable1(
    val parent: SymTable1? = null
) {
    private val symbols = mutableMapOf<String, Symbol>()

    fun define(sym: Symbol) : Symbol? {
        val name = sym.name
        val definedSym = resolve(name)
        if (definedSym != null) {
            when (definedSym) {
                is FuncSymbol -> {
                    if (sym !is FuncSymbol) return null

                    definedSym.definitions.addAll(sym.definitions)
                }
                else -> return null
            }
        }

        symbols[name] = sym
        return sym
    }

    private fun isDefined(name: String): Boolean =
        resolve(name) != null

    private fun isIncomplete(name: String): Boolean =
        resolve(name) is IncompleteSymbol



    fun resolve(name: String): Symbol? =
        symbols[name] ?: parent?.resolve(name)
}*/
