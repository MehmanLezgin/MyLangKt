package lang.core

class Timing {
    private var startTime: Long = current

    val current: Long
        get() = System.currentTimeMillis()

    val timeMillis: Long
        get() = current - startTime

    val timeSeconds: Float
        get() = timeMillis / 1000F

    fun begin() {
        startTime = current
    }
}