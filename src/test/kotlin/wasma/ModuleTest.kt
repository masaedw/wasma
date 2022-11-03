package wasma

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

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
            // add.wasm
            val data = """
                00 61 73 6d 01 00 00 00  01 07 01 60 02 7f 7f 01
                7f 03 02 01 00 07 07 01  03 61 64 64 00 00 0a 09
                01 07 00 20 00 20 01 6a  0b
            """.decodeHex()

            val actual = ModuleLoader.load(data)

            actual.imports shouldBe emptyList()
            actual.exports shouldBe listOf(ModuleExportDescriptor("add", ImportExportKind.FUNCTION, 0))
            actual.functions.map { it.body } shouldBe listOf(intArrayOf(0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b))
        }

        // getAnswerPlus1.wasm
        val getAnswerPlus1 = """
                00 61 73 6d 01 00 00 00  01 05 01 60 00 01 7f 03
                03 02 00 00 07 12 01 0e  67 65 74 41 6e 73 77 65
                72 50 6c 75 73 31 00 01  0a 0e 02 04 00 41 2a 0b
                07 00 10 00 41 01 6a 0b
            """.decodeHex()

        test("multiple functions") {
            val actual = ModuleLoader.load(getAnswerPlus1)

            actual.exports shouldHaveSize 1
            actual.exports[0] shouldBe ModuleExportDescriptor("getAnswerPlus1", ImportExportKind.FUNCTION, 1)

            actual.functions shouldHaveSize 2
            actual.functions[1].body shouldBe intArrayOf(0x00, 0x10, 0x00, 0x41, 0x01, 0x6a, 0x0b)
        }

        test("function types") {
            val actual = ModuleLoader.load(getAnswerPlus1)

            actual.functions[1].type shouldBe Type.Function(listOf(), listOf(Type.I32))
            actual.functions[1].numLocals shouldBe 0
        }
    }
})
