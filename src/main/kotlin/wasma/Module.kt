package wasma

enum class ImportExportKind(val code: Int) {
    FUNCTION(0),
    TABLE(1),
    MEMORY(2),
    GLOBAL(3),
    ;

    companion object {
        fun fromCode(code: Int): ImportExportKind {
            return values().firstOrNull { it.code == code }
                ?: throw UnknownKindException("unknown kind: $code")
        }
    }
}

enum class GlobalMutability(val code: Int) {
    IMMUTABLE(0),
    MUTABLE(1),
    ;

    companion object {
        fun fromCode(code: Int): GlobalMutability {
            return values().firstOrNull() { it.code == code }
                ?: throw InvalidFormatException("unknown mutability: $code")
        }
    }
}

data class ModuleExportDescriptor(
    val name: String,
    val kind: ImportExportKind,
    val index: Int,
)

sealed interface Import {
    val module: String
    val name: String

    data class Function(
        override val module: String,
        override val name: String,
        val type: Type,
    ) : Import

    data class Global(
        override val module: String,
        override val name: String,
        val type: Type,
        val mutability: GlobalMutability,
    ) : Import

    data class Memory(
        override val module: String,
        override val name: String,
        val initial: Int,
    ) : Import

    data class Table(
        override val module: String,
        override val name: String,
        val initial: Int,
    ) : Import
}

class Module(
    val exports: List<ModuleExportDescriptor>,
    val imports: List<Import>,
    val types: List<Type>,
    val functions: List<Function>,
    val table: Int?,
    val elems: List<Int>,
    val data: List<Data>,
)
