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

        val result = target.execute(0, longArrayOf(3, 5))

        result[0] shouldBe 8
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

        val result = target.execute(1, longArrayOf())
        result[0] shouldBe 43
    }

    test("call external functions") {
        // import.wasm
        val data = """
            00000000  00 61 73 6d 01 00 00 00  01 08 02 60 01 7f 00 60  |.asm.......`...`|
            00000010  00 00 02 0f 01 07 63 6f  6e 73 6f 6c 65 03 6c 6f  |......console.lo|
            00000020  67 00 00 03 02 01 01 07  09 01 05 6c 6f 67 49 74  |g..........logIt|
            00000030  00 01 0a 08 01 06 00 41  0d 10 00 0b              |.......A....|
        """.decodeHexdump()

        val m = Loader.load(data)
        var passedValue: Long = 0
        val consoleLog = { ps: LongArray -> passedValue = ps[0]; LongArray(0) }
        val target = Instance(m, mapOf("console" to mapOf("log" to ImportObject.Function(consoleLog))))

        val result = target.execute(1, longArrayOf())

        result.size shouldBe 0
        passedValue shouldBe 13
    }

    test("write global") {
        // global.wasm
        val data = """
                00000000  00 61 73 6d 01 00 00 00  01 08 02 60 00 01 7f 60  |.asm.......`...`|
                00000010  00 00 02 0e 01 02 6a 73  06 67 6c 6f 62 61 6c 03  |......js.global.|
                00000020  7f 01 03 03 02 00 01 07  19 02 09 67 65 74 47 6c  |...........getGl|
                00000030  6f 62 61 6c 00 00 09 69  6e 63 47 6c 6f 62 61 6c  |obal...incGlobal|
                00000040  00 01 0a 10 02 04 00 23  00 0b 09 00 23 00 41 01  |.......#....#.A.|
                00000050  6a 24 00 0b                                       |j${'$'}..|
            """.decodeHexdump()
        val m = Loader.load(data)
        val g = Global.MutableInt(32)
        val target = Instance(m, mapOf("js" to mapOf("global" to ImportObject.Global(g))))

        // getGlobal
        val result1 = target.execute(0, longArrayOf())
        result1[0] shouldBe 32

        // incGlobal
        val result2 = target.execute(1, longArrayOf())
        result2.size shouldBe 0
        g.v shouldBe 33
    }

    test("memory") {
        // import.wasm
        val buf = """
                00000000  00 61 73 6d 01 00 00 00  01 09 02 60 02 7f 7f 00  |.asm.......`....|
                00000010  60 00 00 02 19 02 07 63  6f 6e 73 6f 6c 65 03 6c  |`......console.l|
                00000020  6f 67 00 00 02 6a 73 03  6d 65 6d 02 00 01 03 02  |og...js.mem.....|
                00000030  01 01 07 0b 01 07 77 72  69 74 65 48 69 00 01 0a  |......writeHi...|
                00000040  0a 01 08 00 41 00 41 02  10 00 0b 0b 08 01 00 41  |....A.A........A|
                00000050  00 0b 02 48 69                                    |...Hi|
            """.decodeHexdump()

        val m = Loader.load(buf)
        val memory = Memory(1, 1)
        var stringValue: String? = null
        val consoleLog =
            { ps: LongArray -> stringValue = String(memory.buffer, ps[0].toInt(), ps[1].toInt()); LongArray(0) }

        val target = Instance(
            m,
            mapOf(
                "js" to mapOf("mem" to ImportObject.Memory(memory)),
                "console" to mapOf("log" to ImportObject.Function(consoleLog))
            )
        )

        memory.buffer.slice(0..1) shouldBe "Hi".toByteArray()
        // writeHi
        val result = target.execute(1, longArrayOf())
        result.size shouldBe 0
        stringValue shouldBe "Hi"
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

        val m = Loader.load(data)

        val target = Instance(m)

        // call $f1
        val result1 = target.execute(2, longArrayOf(0))
        result1[0] shouldBe 42

        // call $f2
        val result2 = target.execute(2, longArrayOf(1))
        result2[0] shouldBe 13
    }
})
