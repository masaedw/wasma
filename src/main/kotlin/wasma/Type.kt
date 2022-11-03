package wasma

sealed class Type {
    object I32 : Type()
    object I64 : Type()
    object F32 : Type()
    object F64 : Type()
    data class Function(val params: List<Type>, val results: List<Type>) : Type()
}
