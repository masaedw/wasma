package wasma

class Instance(
    private val m: Module,
    private val imports: Map<String, Map<String, ImportObject>> = mapOf(),
) {
    val stack: LongArray = LongArray(1000)

    // registers
    private var fi: Int = 0 // function index
    private var sp: Int = 0 // stack pointer
    private var fp: Int = 0 // frame pointer
    private var pc: Int = 0 // program counter

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

    private fun mergeFunctions(): List<FunctionLike> {
        val externals =
            m.imports
                .filter { it.kind == ImportExportKind.FUNCTION }
                .map {
                    val external = imports[it.module]?.get(it.name)
                        ?: throw MissingImportException("missing import: ${it.module}.${it.name}")
                    val f = (external as? ImportObject.Function)?.f
                        ?: throw MissingImportException("invalid import: not a function: ${it.module}.${it.name}")
                    ExternalFunction(f, it.type as Type.Function)
                }
        return externals + m.functions
    }

    private val functions = mergeFunctions()


    private fun setupGlobals(): List<Global> {
        return m.imports
            .filter { it.kind == ImportExportKind.GLOBAL }
            .map {
                val external = imports[it.module]?.get(it.name)
                    ?: throw MissingImportException("missing import: ${it.module}.${it.name}")
                val g = (external as? ImportObject.Global)?.g
                    ?: throw MissingImportException("invalid import: not a global: ${it.module}.${it.name}")
                if (it.mutability != g.mutability) {
                    throw MissingImportException("invalid mutability: required ${it.mutability} but ${g.mutability}: ${it.module}.${it.name}")
                }
                g
            }
        // TODO: export and internal globals
    }

    private val globals = setupGlobals()

    private val f: Function
        get() = functions[fi] as Function

    private fun push(v: Long) {
        stack[sp++] = v
    }

    private fun pop(): Long = stack[--sp]

    private fun pushI(v: Int) = push(v.toLong())
    private fun popI(): Int = pop().toInt()

    private fun call(fIndex: Int) {
        when (val f = functions[fIndex]) {
            is Function -> {
                sp += f.numLocals
                pushI(fi)
                pushI(pc)
                pushI(fp)
                fi = fIndex
                pc = 1
                fp = sp - 3 - f.offset
            }

            is ExternalFunction -> {
                val size = f.type.params.size
                sp -= size
                val params = LongArray(size) { stack[sp + it] }
                val result = f.f(params)
                result.forEach { push(it) }
            }
        }
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

    private fun getLocal(i: Int) = stack[fp + i]

    private fun getGlobal(i: Int) = globals[i].value

    private fun setGlobal(i: Int, v: Long) {
        val g = globals[i] as? Global.MutableGlobal
            ?: throw InvalidOperationException("try to write immutable global: $i")
        g.value = v
    }

    fun next() = f.body[pc++]

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

                // global.get
                0x23 -> push(getGlobal(next()))

                // global.set
                0x24 -> setGlobal(next(), pop())

                // i32.const
                0x41 -> pushI(next())

                // i32.add
                0x6a -> pushI(popI() + popI())

                else -> throw UnsupportedOperationException("unknown insn: $insn (0x${insn.toString(16)})")
            }
        }
    }
}
