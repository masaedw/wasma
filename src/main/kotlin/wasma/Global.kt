package wasma

sealed class Global {
    class ImmutableInt(val value: Int) : Global()
    class MutableInt(var value: Int) : Global()
}
