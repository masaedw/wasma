package wasma

class Instance(
    private val m: Module,
    private val imports: Map<String, Map<String, ImportObject>> = mapOf(),
) {
    val stack: LongArray = LongArray(1024 * 8)

    // registers
    private var fi: Int = -1 // function index
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
                .filterIsInstance<Import.Function>()
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
            .filterIsInstance<Import.Global>()
            .map {
                val external = imports[it.module]?.get(it.name)
                    ?: throw MissingImportException("missing import: ${it.module}.${it.name}")
                val g = (external as? ImportObject.Global)?.g
                    ?: throw MissingImportException("invalid import: not a global: ${it.module}.${it.name}")
                if (it.mutability != g.mutability) {
                    throw MissingImportException("invalid mutability: required ${it.mutability} but actual ${g.mutability}: ${it.module}.${it.name}")
                }
                g
            }
        // TODO: export and internal globals
    }

    private val globals = setupGlobals()

    private fun setupMemories(): List<Memory> {
        // TODO: メモリは1つ
        val memories = m.imports
            .filterIsInstance<Import.Memory>()
            .map {
                val external = imports[it.module]?.get(it.name)
                    ?: throw MissingImportException("missing import: ${it.module}.${it.name}")

                val m = (external as? ImportObject.Memory)?.m
                    ?: throw MissingImportException("invalid import: not a memory: ${it.module}.${it.name}")

                if (m.initial < it.initial)
                    throw MissingImportException("too small memory: ${it.module}.${it.name}; required ${it.initial} but actual ${m.initial}")

                m
            }

        m.data.forEach {
            System.arraycopy(it.data, 0, memories[it.index].buffer, it.index, it.data.size)
        }

        return memories
    }

    private val memories = setupMemories()

    private fun setupTable(): Table {
        val t = m.imports
            .filterIsInstance<Import.Table>()
            .map {
                val external = imports[it.module]?.get(it.name)
                    ?: throw MissingImportException("missing import: ${it.module}.${it.name}")

                val t = (external as? ImportObject.Table)?.t
                    ?: throw MissingImportException("invalid import: not a table: ${it.module}.${it.name}")

                if (t.elems.size < it.initial)
                    throw MissingImportException("too small table: ${it.module}.${it.name}; required ${it.initial} but actual ${t.elems.size}")

                t
            }.firstOrNull()

        val pairs = m.elems.map { this to it }

        val table = if (t == null) {
            val initial = m.table ?: 0
            Table(MutableList(initial) { null })
        } else {
            t
        }

        pairs.forEachIndexed { index, pair ->
            table.elems[index] = pair
        }

        return table
    }

    private val table = setupTable()

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

    private fun callIndirect(index: Int, signature: Int, tableIndex: Int) {
        if (tableIndex != 0)
            throw InvalidOperationException("call_indirect: table index must be 0")

        val (instance, fIndex) = table.elems.getOrNull(index)
            ?: throw InvalidOperationException("call_indirect: out of bounds table access, table size: ${table.elems.size} operand: $index")

        val type = m.types[signature] as Type.Function
        val size = type.params.size
        sp -= size
        val params = LongArray(size) { stack[sp + it] }
        val result = instance.execute(fIndex, params)
        if (result.size != type.results.size) {
            throw InvalidOperationException("call_indirect: different num of return values")
        }
        result.forEach { push(it) }
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

    private fun getMemoryI(alignment: Int, offset: Int) {
        val address = popI()
        val data = memories[0].byteBuffer.getInt(address + offset)
        pushI(data)
    }

    private fun setMemoryI(alignment: Int, offset: Int) {
        val value = popI()
        val address = popI()
        memories[0].byteBuffer.putInt(address + offset, value)
    }

    fun next() = f.body[pc++]

    fun execute(fIndex: Int, locals: LongArray): LongArray {
        val requiredSize = functions[fIndex].type.params.size
        if (requiredSize != locals.size) {
            throw InvalidOperationException("different num of parameters: required $requiredSize but actual ${locals.size}")
        }

        locals.forEach { push(it) }
        // callIndirectで呼ばれる場合にはこの時点でfiが-1ではないので復帰先を退避しておく
        val preFi = fi
        fi = -1
        call(fIndex)

        while (true) {
            when (val insn = next()) {
                // end
                0x0b -> {
                    ret()
                    if (fi == -1) {
                        fi = preFi
                        return LongArray(functions[fIndex].type.results.size) { pop() }
                    }
                }
                // call
                0x10 -> call(next())

                // call_indirect
                0x11 -> callIndirect(popI(), next(), next())

                // local.get
                0x20 -> push(getLocal(next()))

                // global.get
                0x23 -> push(getGlobal(next()))

                // global.set
                0x24 -> setGlobal(next(), pop())

                // i32.load
                0x28 -> getMemoryI(next(), next())

                // i32.store
                0x36 -> setMemoryI(next(), next())

                // i32.const
                0x41 -> pushI(next())

                // i32.add
                0x6a -> pushI(popI() + popI())

                else -> throw InvalidOperationException("unknown insn: $insn (0x${insn.toString(16)})")
            }
        }
    }
}
