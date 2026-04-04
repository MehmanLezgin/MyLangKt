package lang.infrastructure

class RangeBuilder(
    val startRange: SourceRange,
    private val endProvider: () -> SourceRange
) {
    val resultRange: SourceRange
        get() = startRange untilEndOf endProvider()
}