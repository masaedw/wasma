package wasma

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VmTest : FunSpec({
    test("execute") {
        // add.wasm
        val data = """
                00 61 73 6d 01 00 00 00  01 07 01 60 02 7f 7f 01
                7f 03 02 01 00 07 07 01  03 61 64 64 00 00 0a 09
                01 07 00 20 00 20 01 6a  0b
            """.decodeHex()

        val m = ModuleLoader.load(data)
        val target = Vm(m)

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

        val m = ModuleLoader.load(data)
        val target = Vm(m)

        target.execute(1, longArrayOf())
        target.stack[0] shouldBe 43
    }
})
