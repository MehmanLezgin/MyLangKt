package lang.mappers

interface IMapper<A, B> {
    fun toFirst(b: B) : A
    fun toSecond(a: A) : B
}

