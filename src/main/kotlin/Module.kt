enum class ImportExportKind {
    FUNCTION,
    TABLE,
    MEMORY,
    GLOBAL,
    ;
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

data class Module(
    val exports: List<ModuleExportDescriptor>,
    val imports: List<ModuleImportDescriptor>,
) {
    companion object {
        fun loadFrom(buffer: ByteArray): Module {
            return Module(emptyList(), emptyList())
        }
    }
}
