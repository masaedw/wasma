package wasma

import java.nio.ByteBuffer

class Memory(val initial: Int, val max: Int) {
    val buffer: ByteArray = ByteArray(initial * 64 * 1024)
    val byteBuffer: ByteBuffer = ByteBuffer.wrap(buffer)
    // todo extend
}
