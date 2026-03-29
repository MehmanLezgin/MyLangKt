package lang.semantics.types

class TypeRef(initial: Type) {
    var value: Type = initial
        private set

    fun resolve(type: Type) {
        if (value != UnresolvedType) {
            error("Return type already resolved")
        }
        value = type
    }
}