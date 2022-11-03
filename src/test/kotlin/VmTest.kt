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
})
