package wasma

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InstanceTest : FunSpec({
    test("execute") {
        // add.wasm
        val data = """
                00 61 73 6d 01 00 00 00  01 07 01 60 02 7f 7f 01
                7f 03 02 01 00 07 07 01  03 61 64 64 00 00 0a 09
                01 07 00 20 00 20 01 6a  0b
            """.decodeHex()

        val m = Loader.load(data)
        val target = Instance(m)

        target.execute(0, longArrayOf(3, 5))

        target.stack[0] shouldBe 8
    }

    test("call") {
        // getAnswerPlus1.wasm
        val data = """
            00000000  00 61 73 6d 01 00 00 00  01 05 01 60 00 01 7f 03  |.asm.......`....|
            00000010  03 02 00 00 07 12 01 0e  67 65 74 41 6e 73 77 65  |........getAnswe|
            00000020  72 50 6c 75 73 31 00 01  0a 0e 02 04 00 41 2a 0b  |rPlus1.......A*.|
            00000030  07 00 10 00 41 01 6a 0b                           |....A.j.|
        """.decodeHexdump()

        val m = Loader.load(data)
        val target = Instance(m)

        target.execute(1, longArrayOf())
        target.stack[0] shouldBe 43
    }

    context("import") {
        // import.wasm
        val data = """
            00000000  00 61 73 6d 01 00 00 00  01 08 02 60 01 7f 00 60  |.asm.......`...`|
            00000010  00 00 02 0f 01 07 63 6f  6e 73 6f 6c 65 03 6c 6f  |......console.lo|
            00000020  67 00 00 03 02 01 01 07  09 01 05 6c 6f 67 49 74  |g..........logIt|
            00000030  00 01 0a 08 01 06 00 41  0d 10 00 0b              |.......A....|
        """.decodeHexdump()

        test("load") {
            val m = Loader.load(data)
            var passedValue: Long = 0
            val consoleLog = { ps: LongArray -> passedValue = ps[0]; LongArray(0) }
            val target = Instance(m, mapOf("console" to mapOf("log" to consoleLog)))

            target.execute(1, longArrayOf())

            passedValue shouldBe 13
        }
    }
})