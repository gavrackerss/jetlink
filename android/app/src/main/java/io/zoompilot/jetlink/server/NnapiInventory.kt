package io.zoompilot.jetlink.server

/** Public Android NNAPI metadata only. No shell, root or vendor library access. */
object NnapiInventory {
    init { System.loadLibrary("g6nnapiinventory") }
    private external fun nativeInventory(): Array<String>

    fun read(): List<NnapiDevice> {
        val fields = nativeInventory()
        check(fields.size % 4 == 0) { "Incomplete NNAPI metadata" }
        return fields.toList().chunked(4).map {
            NnapiDevice(it[0], it[1].toInt(), it[2], it[3].toLong())
        }
    }
}
