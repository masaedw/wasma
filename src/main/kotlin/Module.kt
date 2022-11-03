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

data class ModuleExportDescriptor(
    val name: String,
    val kind: ImportExportKind,
)

data class ModuleImportDescriptor(
    val module: String,
    val name: String,
    val kind: ImportExportKind,
)

class Module internal constructor(
    val exports: List<ModuleExportDescriptor>,
    val imports: List<ModuleImportDescriptor>,
    val functions: List<Function>,
)

class UnsupportedSectionException(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)

class UnknownKindException(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)
