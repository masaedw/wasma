package wasma

class Function(
    val body: IntArray,
    override val type: Type.Function,
) : FunctionLike {
    val offset: Int
        get() = numLocals + type.params.size

    val numLocals: Int
        get() = body[0]
}
