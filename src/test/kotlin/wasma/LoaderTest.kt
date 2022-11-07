package wasma

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class LoaderTest : FunSpec({
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

            val actual = Loader.load(data)

            actual.imports shouldBe emptyList()
            actual.exports shouldBe emptyList()
        }

        test("import") {
            val data = """
                00000000  00 61 73 6d 01 00 00 00  01 08 02 60 01 7f 00 60  |.asm.......`...`|
                00000010  00 00 02 0f 01 07 63 6f  6e 73 6f 6c 65 03 6c 6f  |......console.lo|
                00000020  67 00 00 03 02 01 01 07  09 01 05 6c 6f 67 49 74  |g..........logIt|
                00000030  00 01 0a 08 01 06 00 41  0d 10 00 0b              |.......A....|
            """.decodeHexdump()

            val actual = Loader.load(data)

            actual.imports shouldHaveSize 1
            actual.imports[0] shouldBe Import.Function(
                "console",
                "log",
                Type.Function(listOf(Type.I32), listOf())
            )
        }

        test("import global") {
            // global.wasm
            val data = """
                00000000  00 61 73 6d 01 00 00 00  01 08 02 60 00 01 7f 60  |.asm.......`...`|
                00000010  00 00 02 0e 01 02 6a 73  06 67 6c 6f 62 61 6c 03  |......js.global.|
                00000020  7f 01 03 03 02 00 01 07  19 02 09 67 65 74 47 6c  |...........getGl|
                00000030  6f 62 61 6c 00 00 09 69  6e 63 47 6c 6f 62 61 6c  |obal...incGlobal|
                00000040  00 01 0a 10 02 04 00 23  00 0b 09 00 23 00 41 01  |.......#....#.A.|
                00000050  6a 24 00 0b                                       |j${'$'}..|
            """.decodeHexdump()

            val actual = Loader.load(data)

            actual.imports shouldHaveSize 1
            actual.imports[0] shouldBe Import.Global(
                "js",
                "global",
                Type.I32,
                GlobalMutability.MUTABLE
            )
        }

        test("import memory") {
            // import.wasm
            val buf = """
                00000000  00 61 73 6d 01 00 00 00  01 09 02 60 02 7f 7f 00  |.asm.......`....|
                00000010  60 00 00 02 19 02 07 63  6f 6e 73 6f 6c 65 03 6c  |`......console.l|
                00000020  6f 67 00 00 02 6a 73 03  6d 65 6d 02 00 01 03 02  |og...js.mem.....|
                00000030  01 01 07 0b 01 07 77 72  69 74 65 48 69 00 01 0a  |......writeHi...|
                00000040  0a 01 08 00 41 00 41 02  10 00 0b 0b 08 01 00 41  |....A.A........A|
                00000050  00 0b 02 48 69                                    |...Hi|
            """.decodeHexdump()

            val actual = Loader.load(buf)

            actual.imports shouldHaveSize 2
            actual.imports[1] shouldBe Import.Memory("js", "mem", 1)
            actual.data shouldHaveSize 1

            val data = actual.data[0]
            data.flags shouldBe 0
            data.index shouldBe 0
            data.data shouldBe "Hi".toByteArray()
        }

        test("export") {
            // add.wasm
            val data = """
                00 61 73 6d 01 00 00 00  01 07 01 60 02 7f 7f 01
                7f 03 02 01 00 07 07 01  03 61 64 64 00 00 0a 09
                01 07 00 20 00 20 01 6a  0b
            """.decodeHex()

            val actual = Loader.load(data)

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
            val actual = Loader.load(getAnswerPlus1)

            actual.exports shouldHaveSize 1
            actual.exports[0] shouldBe ModuleExportDescriptor("getAnswerPlus1", ImportExportKind.FUNCTION, 1)

            actual.functions shouldHaveSize 2
            actual.functions[1].body shouldBe intArrayOf(0x00, 0x10, 0x00, 0x41, 0x01, 0x6a, 0x0b)
        }

        test("function types") {
            val actual = Loader.load(getAnswerPlus1)

            actual.functions[1].type shouldBe Type.Function(listOf(), listOf(Type.I32))
            actual.functions[1].numLocals shouldBe 0
        }

        test("table") {
            // wasm-table.wasm
            val data = """
                00000000  00 61 73 6d 01 00 00 00  01 0a 02 60 00 01 7f 60  |.asm.......`...`|
                00000010  01 7f 01 7f 03 04 03 00  00 01 04 04 01 70 00 02  |.............p..|
                00000020  07 0f 01 0b 63 61 6c 6c  42 79 49 6e 64 65 78 00  |....callByIndex.|
                00000030  02 09 08 01 00 41 00 0b  02 00 01 0a 13 03 04 00  |.....A..........|
                00000040  41 2a 0b 04 00 41 0d 0b  07 00 20 00 11 00 00 0b  |A*...A.... .....|
            """.decodeHexdump()

            val actual = Loader.load(data)

            actual.table shouldBe listOf(0, 1)
        }
    }
})
