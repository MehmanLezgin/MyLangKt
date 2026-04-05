package lang.semantics.symbols

import lang.semantics.types.TemplateArg
import lang.semantics.types.TemplateParam

data class TemplateParamSymbol(
    val param: TemplateParam
) : Symbol {
    override val name: String = param.name
    override val modifiers: Modifiers = Modifiers()
}

data class TemplateArgSymbol(
    override val name: String,
    val arg: TemplateArg
) : Symbol {
    override val modifiers: Modifiers = Modifiers()
}

//sealed class TemplateParamSymbol(
//    override val modifiers: Modifiers = Modifiers()
//) : Symbol() {
//    class TypeParamSymbol(
//        override val name: String,
//        val bound: Type?
//    ) : TemplateParamSymbol()
//
//    class ConstParamSymbol(
//        override val name: String,
//        val type: Type
//    ) : TemplateParamSymbol()
//}