package wasma

class Table(
    val elems: MutableList<Pair<Instance, Int>?>,
) {
    constructor(initial: Int) : this(MutableList(initial) { null })
}
