import java.io.EOFException
import java.io.IOException

class ModuleLoader(
    private val buffer: ByteArray,
) {
    private var exports: List<ModuleExportDescriptor>? = null
    private var imports: List<ModuleImportDescriptor>? = null
    private var types: List<Type>? = null
    private var functionTypes: List<Type.Function>? = null
    private var functions: List<Function>? = null

    private var pos = 0

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

    fun load(): Module {
        skip(4) // magic
        skip(4) // version

        while (true) {
            when (readOrEof()) {
                1 -> loadTypeSection()
                3 -> loadFunctionSection()
                7 -> loadExportSection()
                10 -> loadCodeSection()
                -1 -> break
                else -> throw UnsupportedSectionException()
            }
        }

        return Module(
            exports ?: emptyList(),
            imports ?: emptyList(),
            types ?: emptyList(),
            functions ?: emptyList(),
        )
    }

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
            else -> throw UnknownTypeException("unknown type: $type")
        }
    }

    private fun loadTypeSection() {
        val size = read()
        types = List(read()) { readType() }
    }

    private fun loadFunctionSection() {
        val size = read()
        val types = types ?: throw InvalidFormatException("missing type section")
        functionTypes = List(read()) {
            types[read()] as Type.Function
        }
    }

    private fun loadExportSection() {
        val size = read()
        exports = List(read()) {
            val name = readString()
            val kind = ImportExportKind.fromCode(read())
            val index = read()
            ModuleExportDescriptor(name, kind, index)
        }
    }

    private fun loadCodeSection() {
        val size = read()
        val functionTypes = functionTypes ?: throw InvalidFormatException("missing function section")

        functions = List(read()) {
            val bodySize = read()

            val body = IntArray(bodySize)

            for (j in 0 until bodySize) {
                body[j] = read()
            }

            Function(body, functionTypes[it])
        }
    }

    companion object {
        fun load(buffer: ByteArray): Module {
            return ModuleLoader(buffer).load()
        }
    }
}
