package wasma

import java.io.EOFException
import java.io.IOException

class Loader(
    private val buffer: ByteArray,
) {
    private var exports: List<ModuleExportDescriptor>? = null
    private var imports: List<Import>? = null
    private var types: List<Type>? = null
    private var functionTypes: List<Type.Function>? = null
    private var table: Table? = null
    private var functions: List<Function>? = null
    private var data: List<Data>? = null

    private var pos = 0

    private fun getType(i: Int): Type {
        val types = types ?: throw InvalidFormatException("missing type section")
        return types[i]
    }

    private fun skip(n: Int) {
        pos += n
    }

    private fun readOrEof(): Int {
        return if (pos < buffer.size)
            buffer[pos++].toInt()
        else
            -1
    }

    private fun read(): Int {
        val n = readOrEof()
        if (n == -1) throw EOFException("pos: $pos")
        // TODO: https://en.wikipedia.org/wiki/LEB128 を読むようにする
        return n
    }

    private fun readString(): String {
        val size = read()
        if (pos + size > buffer.size) {
            throw IOException("buffer is too short.  pos: $pos, size: $size")
        }
        val str = String(buffer, pos, size)
        pos += size
        return str
    }

    private fun readBytes(len: Int): ByteArray {
        val buf = buffer.sliceArray(pos until pos + len)
        pos += len
        return buf
    }

    fun load(): Module {
        skip(4) // magic
        skip(4) // version

        while (true) {
            when (val section = readOrEof()) {
                1 -> types = readTypeSection()
                2 -> imports = readImportSection()
                3 -> functionTypes = readFunctionSection()
                4 -> table = readTable()
                7 -> exports = readExportSection()
                9 -> readElem()
                10 -> functions = readCodeSection()
                11 -> data = readDataSection()
                -1 -> break
                else -> throw UnsupportedSectionException("unknown section: $section")
            }
        }

        return Module(
            exports ?: emptyList(),
            imports ?: emptyList(),
            types ?: emptyList(),
            functions ?: emptyList(),
            table?.elems ?: emptyList(),
            data ?: emptyList()
        )
    }

    private fun readKind() = ImportExportKind.fromCode(read())

    private fun readType(): Type {
        return when (val type = read()) {
            // func
            0x60 -> {
                val params = List(read()) { readType() }
                val results = List(read()) { readType() }
                Type.Function(params, results)
            }
            // i32
            0x7f -> Type.I32
            else -> throw UnknownTypeException("unknown type: $type (0x${type.toString(16)})")
        }
    }

    private fun readTypeSection(): List<Type> {
        read() // skip size
        return List(read()) { readType() }
    }

    private fun readMutability() = GlobalMutability.fromCode(read())

    private fun readImport(): Import {
        val module = readString()
        val name = readString()

        return when (val kind = readKind()) {
            ImportExportKind.FUNCTION -> {
                val type = getType(read())
                Import.Function(module, name, type)
            }

            ImportExportKind.GLOBAL -> {
                val type = readType()
                val mutability = readMutability()
                Import.Global(module, name, type, mutability)
            }

            ImportExportKind.MEMORY -> {
                val flags = read() // todo take care of flags
                val initial = read()
                Import.Memory(module, name, initial)
            }

            ImportExportKind.TABLE,
            -> TODO()
        }
    }


    private fun readImportSection(): List<Import> {
        read() // skip size
        return List(read()) { readImport() }
    }

    private fun readFunctionSection(): List<Type.Function> {
        read() // skip size
        return List(read()) { getType(read()) as Type.Function }
    }

    private fun readTable(): Table {
        read() // skip size
        if (read() != 1) // num tables
            throw InvalidFormatException("table size should be 1 for now")

        when (val type = read()) {
            0x70 -> {
                read() // flags
                val initial = read()
                return Table(MutableList(initial) { -1 }) // TODO: implement call on throw
            }

            else -> throw InvalidFormatException("unknown table type: $type (0x${type.toString(16)})")
        }
    }

    private fun readExport() =
        ModuleExportDescriptor(readString(), readKind(), read())

    private fun readExportSection(): List<ModuleExportDescriptor> {
        read() // skip size
        return List(read()) { readExport() }
    }

    data class SegmentHeader(
        val flags: Int,
        val index: Int,
    )

    private fun readSegmentHeader(): SegmentHeader {
        val flags = read()
        var index = 0
        while (true) {
            when (val insn = read()) {
                // i32.const
                0x41 -> index = read()
                // end
                0x0b -> break
                else -> throw UnsupportedOperationException("failed to read segment header: $insn (0x${insn.toString(16)})")
            }
        }
        return SegmentHeader(flags, index)
    }

    private fun readElem() {
        read() // skip size
        val numSegments = read()
        if (numSegments != 1)
            throw InvalidFormatException("num of elem segments must be 1")

        val header = readSegmentHeader()
        if (header.flags != 0)
            throw InvalidFormatException("elem section segment header must be 0")

        val size = read()
        val table = table ?: throw InvalidFormatException("missing table section")
        (0 until size).forEach {
            table.elems[header.index + it] = read()
        }
    }

    private fun readCodeSection(): List<Function> {
        read() // skip size
        val functionTypes = functionTypes ?: throw InvalidFormatException("missing function section")
        return List(read()) {
            val body = IntArray(read()) { read() }
            Function(body, functionTypes[it])
        }
    }

    private fun readData(): Data {
        val header = readSegmentHeader()
        val size = read()
        return Data(header.flags, header.index, readBytes(size))
    }

    private fun readDataSection(): List<Data> {
        read() // skip size
        return List(read()) { readData() }
    }

    companion object {
        fun load(buffer: ByteArray): Module {
            return Loader(buffer).load()
        }
    }
}
