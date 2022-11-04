package wasma

sealed interface Global {
    val mutability: GlobalMutability
        get() = GlobalMutability.IMMUTABLE
    val value: Long

    sealed interface MutableGlobal : Global {
        override val mutability: GlobalMutability
            get() = GlobalMutability.MUTABLE
        override var value: Long
    }

    class ImmutableInt(v: Int) : Global {
        override val value = v.toLong()
    }

    class MutableInt(var v: Int) : MutableGlobal {
        override var value: Long
            get() = v.toLong()
            set(value) {
                v = value.toInt()
            }
    }
}
