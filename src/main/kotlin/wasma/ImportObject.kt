package wasma

sealed interface ImportObject {
    class Function(val f: (LongArray) -> LongArray) : ImportObject
    class Global(val g: wasma.Global) : ImportObject
}
