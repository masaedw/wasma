package wasma

/**
 * convert hex string separated by space to UByteArray
 */
fun String.decodeHex(): ByteArray {
    val dense = Regex("\\s+").replace(this, "")

    return ByteArray(dense.length / 2) {
        Integer.parseInt(dense, it * 2, (it + 1) * 2, 16).toByte()
    }
}

/**
 * convert hex string separated by space to UByteArray
 */
fun String.decodeHexdump(): ByteArray {
    val hex = Regex("\\w+\\s+(.*)\\|.+").replace(this, "$1")
    return hex.decodeHex()
}
