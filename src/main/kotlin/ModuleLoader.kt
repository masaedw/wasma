import java.io.EOFException
import java.io.IOException

class ModuleLoader(
    private val buffer: ByteArray,
) {
    private var exports: List<ModuleExportDescriptor>? = null
    private var imports: List<ModuleImportDescriptor>? = null

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
        return n
    }

    private fun readUByte(): UByte = read().toUByte()

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

        return Module(exports ?: emptyList(), imports ?: emptyList())
    }

    private fun loadTypeSection() {
        val size = readUByte()
        skip(size.toInt())
        // TODO: Not yet implemented
    }

    private fun loadFunctionSection() {
        val size = read()
        skip(size)
        // TODO: Not yet implemented
    }

    private fun loadExportSection() {
        val size = readUByte()
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
        skip(size)
        // TODO: Not yet implemented
    }

    companion object {
        fun load(buffer: ByteArray): Module {
            return ModuleLoader(buffer).load()
        }
    }
}
