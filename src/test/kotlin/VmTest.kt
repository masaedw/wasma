import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VmTest : FunSpec({
    test("execute") {
        // add.wasm
        val f = Function(intArrayOf(0x00, 0x20, 0x00, 0x20, 0x01, 0x6a, 0x0b))
        val target = Vm(f, longArrayOf(3, 5))

        target.execute()

        target.stack[0] shouldBe 8
    }
})
