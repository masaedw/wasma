package wasma

class Memory(val initial: Int, val max: Int) {
    val buffer: ByteArray = ByteArray(initial * 64 * 1024)
    // todo extend
}
