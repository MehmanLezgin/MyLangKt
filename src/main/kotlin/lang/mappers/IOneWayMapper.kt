package lang.mappers

interface IOneWayMapper<A, B> {
    fun toSecond(a: A) : B
}