class Function(
    val body: IntArray,
    val type: Type.Function,
) {
    val numLocals: Int
        get() = body[0]
}
