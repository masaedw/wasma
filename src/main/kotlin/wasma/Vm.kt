package wasma

class Vm(
    private val m: Module,
) {
    val stack: LongArray = LongArray(1000)

    // registers
    var fi: Int = 0 // function index
    var sp: Int = 0 // stack pointer
    var fp: Int = 0 // frame pointer
    var pc: Int = 0 // program counter

    // the stack and frame structure
    //
    // extend to top
    //
    //             <- sp
    // fp          <- fp + offset + 2
    // pc          <- fp + offset + 1
    // fi          <- fp + offset
    // locals
    // parameters  <- fp
    //

    private val f: Function
        get() = m.functions[fi]


    private fun push(v: Long) {
        stack[sp++] = v
    }

    private fun pop(): Long = stack[--sp]

    private fun pushI(v: Int) = push(v.toLong())
    private fun popI(): Int = pop().toInt()

    private fun call(fIndex: Int) {
        sp += m.functions[fIndex].numLocals
        pushI(fi)
        pushI(pc)
        pushI(fp)
        fi = fIndex
        pc = 1
        fp = sp - 3 - f.offset
    }

    private fun ret() {
        val offset = f.offset
        val results = f.type.results.size
        val fp = fp
        fi = stack[fp + offset].toInt()
        pc = stack[fp + offset + 1].toInt()
        this.fp = stack[fp + offset + 2].toInt()

        // 戻り値をstackに戻す
        // TODO: 複合型があった場合サイズが変わる
        (0 until results).forEach {
            stack[fp + it] = stack[sp - results + it]
        }
        sp = fp + results
    }

    private inline fun getLocal(i: Int) = stack[fp + i]

    private inline fun next() = f.body[pc++]

    fun execute(fIndex: Int, locals: LongArray) {
        sp = 0
        fp = 0
        pc = 1
        fi = fIndex
        locals.forEach { push(it) }
        sp += f.numLocals
        pushI(-1) // fi; return to outside vm
        pushI(0) // pc
        pushI(0)  // fp

        while (true) {
            when (val insn = next()) {
                // end
                0x0b -> {
                    ret()
                    if (fi == -1) return
                }
                // call
                0x10 -> call(next())

                // local.get
                0x20 -> push(getLocal(next()))

                // i32.const
                0x41 -> pushI(next())

                // i32.add
                0x6a -> pushI(popI() + popI())

                else -> throw UnsupportedOperationException("unknown insn: 0x${insn.toString(16)}")
            }
        }
    }
}
