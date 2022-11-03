import java.io.EOFException
import java.io.IOException

class ModuleLoader(
    private val buffer: ByteArray,
) {
    private var exports: List<ModuleExportDescriptor>? = null
    private var imports: List<ModuleImportDescriptor>? = null
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
            functions ?: emptyList()
        )
    }

    private fun loadTypeSection() {
        val size = read()
        skip(size)
        // TODO: Not yet implemented
    }

    private fun loadFunctionSection() {
        val size = read()
        skip(size)
        // TODO: Not yet implemented
    }

    private fun loadExportSection() {
        val size = read()
        val numExports = read()
        exports = buildList {
            for (i in 0 until numExports) {
                val name = readString()
                val kind = ImportExportKind.fromCode(read())
                val index = read()
                add(ModuleExportDescriptor(name, kind))
            }
        }
    }

    private fun loadCodeSection() {
        val size = read()
        val numCodes = read()
        functions = buildList {
            for (i in 0 until numCodes) {
                val bodySize = read()

                val body = IntArray(bodySize)

                for (j in 0 until bodySize) {
                    body[j] = read()
                }

                add(Function(body))
            }
        }
        // TODO: Not yet implemented
    }

    companion object {
        fun load(buffer: ByteArray): Module {
            return ModuleLoader(buffer).load()
        }
    }
}
