package wasma

class ExternalFunction(
    val f: (LongArray) -> LongArray,
    val type: Type.Function,
) : FunctionLike

