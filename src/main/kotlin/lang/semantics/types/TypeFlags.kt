package lang.semantics.types

data class TypeFlags(
    val isConst: Boolean = false,
    val isLvalue: Boolean = false,
    val isExprType: Boolean = false,
    val isMutable: Boolean = false
)