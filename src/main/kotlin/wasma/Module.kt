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

data class ModuleImportDescriptor(
    val module: String,
    val name: String,
    val kind: ImportExportKind,
    val type: Type,
    val mutability: GlobalMutability,
)

class Module(
    val exports: List<ModuleExportDescriptor>,
    val imports: List<ModuleImportDescriptor>,
    val types: List<Type>,
    val functions: List<Function>,
)
