package wasma

class UnsupportedSectionException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class UnknownKindException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class UnknownTypeException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class InvalidFormatException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class MissingImportException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class InvalidOperationException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)
