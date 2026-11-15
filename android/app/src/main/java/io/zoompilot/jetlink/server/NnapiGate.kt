package io.zoompilot.jetlink.server

data class NnapiDevice(val name: String, val type: Int, val version: String, val feature: Long)

/**
 * ORT's CPU_DISABLED selects non-CPU NNAPI devices, which can include a GPU.
 * Proceed only when that entire eligible set is Google's EdgeTPU accelerator.
 * This establishes the requested driver, not model support or output accuracy.
 */
object NnapiGate {
    fun blocker(devices: List<NnapiDevice>): String? {
        val eligible = devices.filter { it.type != 2 }
        if (eligible.isEmpty()) return "No non-CPU NNAPI device exposed. Full LS2 compilation was not attempted."
        if (eligible.any { it.type != 4 || it.name != "google-edgetpu" }) {
            return "Cannot isolate google-edgetpu with this NNAPI backend. Eligible devices: " +
                eligible.joinToString { "${it.name} (type ${it.type})" } + ". Full LS2 compilation was not attempted."
        }
        return null
    }
}
