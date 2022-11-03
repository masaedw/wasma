class Vm(
    private val f: Function,
    private val locals: LongArray,
) {
    val stack: LongArray = LongArray(500)
    var sp: Int = 0
    var pc: Int = 1 // skip decl count

    private fun push(v: Long) {
        stack[sp++] = v
    }

    private fun pop(): Long = stack[--sp]

    private fun pushI(v: Int) = push(v.toLong())
    private fun popI(): Int = pop().toInt()

    fun execute() {
        while (true) {
            when (val insn = f.body[pc++]) {
                // end
                0x0b -> return
                // local.get
                0x20 -> {
                    val i = f.body[pc++]
                    push(locals[i])
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
