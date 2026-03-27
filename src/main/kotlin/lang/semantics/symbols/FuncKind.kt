package lang.semantics.symbols

import lang.messages.Terms

enum class FuncKind(val kindName: String) {
    FUNCTION(Terms.FUNCTION),
    OPERATOR(Terms.OPERATOR),
    CONSTRUCTOR(Terms.CONSTRUCTOR),
    DESTRUCTOR(Terms.DESTRUCTOR)
}