package lang.semantics.builtin.builders

import lang.core.operators.OperatorType
import lang.semantics.scopes.BaseTypeScope
import lang.semantics.scopes.Scope
import lang.semantics.symbols.FuncSymbol
import lang.semantics.symbols.Modifiers
import lang.semantics.symbols.TypeSymbol
import lang.semantics.symbols.VarSymbol
import lang.semantics.types.ConstValue
import lang.semantics.types.Type

abstract class BaseTypeBuilder<T: TypeSymbol>(open val name: String, open val parent: Scope) {
    internal var superTypeScope: BaseTypeScope? = null
    internal var modifiers: Modifiers = Modifiers()

    open lateinit var typeScope: BaseTypeScope

    abstract fun build(): T

    fun modifiers(modifiers: Modifiers) {
        this.modifiers = modifiers
    }

    fun superTypeScope(superTypeScope: BaseTypeScope) {
        this.superTypeScope = superTypeScope
    }

    fun withStaticScope(block: Scope.() -> Unit) {
        typeScope.apply(block)
    }

    fun withInstanceScope(block: Scope.() -> Unit) {
        typeScope.instanceScope.apply(block)
    }

    fun staticFunc(name: String, block: FuncBuilder.() -> Unit): FuncSymbol {
        val sym = FuncBuilder(name).apply(block).build()
        withStaticScope { defineFunc(sym) }
        return sym
    }

    fun instanceFunc(name: String, block: FuncBuilder.() -> Unit): FuncSymbol {
        val sym = FuncBuilder(name).apply(block).build()
        withInstanceScope { defineFunc(sym) }
        return sym
    }

    fun operFunc(oper: OperatorType, block: FuncBuilder.() -> Unit): FuncSymbol {
        val builder = FuncBuilder(oper)

        val sym = builder.apply(block).build()
        withStaticScope { defineFunc(sym) }
        return sym
    }

    fun staticConstVar(
        name: String,
        type: Type,
        value: ConstValue<*>,
        block: ConstVarBuilder.() -> Unit = {}
    ): VarSymbol {
        val sym = ConstVarBuilder(name, type, value).apply(block).build()
        withStaticScope { define(sym) }
        return sym
    }
}

