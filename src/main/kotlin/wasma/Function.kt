package wasma

class Function(
    val body: IntArray,
    val type: Type.Function,
) {
    val offset: Int
        get() = numLocals + type.params.size

    val numLocals: Int
        get() = body[0]
}
