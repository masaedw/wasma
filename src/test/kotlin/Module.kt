import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * convert hex string separated by space to UByteArray
 */
fun String.decodeHex(): ByteArray {
    val dense = this.replace(" ", "")

    return ByteArray(dense.length / 2) {
        Integer.parseInt(dense, it * 2, (it + 1) * 2, 16).toByte()
    }
}

class ModuleTest : FunSpec({
    test("decodeHex") {
        val bytes = "00 61 73 6d 01 00 00 00".decodeHex()
        bytes shouldBe byteArrayOf(0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00)
    }

    context("load") {
        test("empty") {
            val data = "00 61 73 6d 01 00 00 00".decodeHex()
            val actual = Module.loadFrom(data)
            actual shouldBe Module(emptyList(), emptyList())
        }
    }
})
