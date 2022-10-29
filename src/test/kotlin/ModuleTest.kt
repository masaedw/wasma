import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * convert hex string separated by space to UByteArray
 */
fun String.decodeHex(): ByteArray {
    val dense = Regex("\\s+").replace(this, "")

    return ByteArray(dense.length / 2) {
        Integer.parseInt(dense, it * 2, (it + 1) * 2, 16).toByte()
    }
}

class ModuleTest : FunSpec({
    test("decodeHex") {
        val bytes = "00 61 73 6d 01 00 00 00".decodeHex()
        bytes shouldBe byteArrayOf(0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00)

        val bytes2 = """
            00 61 73 6d 01 00 00 00  01 07 01 60 02 7f 7f 01
            7f 03 02 01 00 07 07 01  03 61 64 64 00 00 0a 09
            01 07 00 20 00 20 01 6a  0b
        """.decodeHex()
        bytes2.sliceArray(0..3) shouldBe byteArrayOf(0x00, 0x61, 0x73, 0x6d)
    }

    context("load") {
        test("empty") {
            val data = "00 61 73 6d 01 00 00 00".decodeHex()

            val actual = ModuleLoader.load(data)

            actual.imports shouldBe emptyList()
            actual.exports shouldBe emptyList()
        }

        test("export") {
            val data = """
                00 61 73 6d 01 00 00 00  01 07 01 60 02 7f 7f 01
                7f 03 02 01 00 07 07 01  03 61 64 64 00 00 0a 09
                01 07 00 20 00 20 01 6a  0b
            """.decodeHex()

            val actual = ModuleLoader.load(data)

            actual.imports shouldBe emptyList()
            actual.exports shouldBe listOf(ModuleExportDescriptor("add", ImportExportKind.FUNCTION))
        }
    }
})