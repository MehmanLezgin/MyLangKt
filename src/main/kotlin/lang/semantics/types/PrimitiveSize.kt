package lang.semantics.types

enum class PrimitiveSize(val bytes: Int) {
    NO_SIZE(bytes = 0),
    BYTE(bytes = 1),
    WORD(bytes = 2),
    DWORD(bytes = 4),
    QWORD(bytes = 8)
}