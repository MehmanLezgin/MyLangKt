package lang.semantics.types

enum class PrimitiveSize(val size: Int) {
    NO_SIZE(size = 0),
    BYTE(size = 1),
    WORD(size = 2),
    DWORD(size = 4),
    QWORD(size = 8)
}