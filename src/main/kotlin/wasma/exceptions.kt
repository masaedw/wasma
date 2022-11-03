package wasma

class UnsupportedSectionException(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)

class UnknownKindException(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)

class UnknownTypeException(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)

class InvalidFormatException(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)
