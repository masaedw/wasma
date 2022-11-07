package wasma

class ExternalFunction(
    val f: (LongArray) -> LongArray,
    override val type: Type.Function,
) : FunctionLike

