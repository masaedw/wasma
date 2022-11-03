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
    // fp          <- fp + 2
    // pc          <- fp + 1
    // function    <- fp
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
        pushI(fi)
        pushI(pc)
        pushI(fp)
        pc = 1
    }

    private fun ret() {
        TODO()
    }

    private inline fun next() = f.body[pc++]

    fun execute(fIndex: Int, locals: LongArray) {
        fi = fIndex
        pc = 1

        while (true) {
            when (val insn = next()) {
                // end
                0x0b -> {
                    ret()
                }
                // call
                0x10 -> {
                    call(next())
                }
                // local.get
                0x20 -> {
                    push(locals[next()])
                }
                // i32.add
                0x6a -> {
                    pushI(popI() + popI())
                }

                else -> throw UnsupportedOperationException("unknown insn: $insn")
            }
        }
    }
}
