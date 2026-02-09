package lang.semantics.symbols

data class Modifiers(
    val visibility: Visibility = Visibility.PUBLIC,
    var isStatic: Boolean = false,
    var isAbstract: Boolean = false,
    var isOpen: Boolean = false,
    var isOverride: Boolean = false,
    var isInfix: Boolean = false
)
