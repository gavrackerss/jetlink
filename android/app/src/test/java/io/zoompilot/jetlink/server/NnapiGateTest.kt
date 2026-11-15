package io.zoompilot.jetlink.server

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NnapiGateTest {
    private val cpu = NnapiDevice("nnapi-reference", 2, "1", 31)
    private val tpu = NnapiDevice("google-edgetpu", 4, "1", 31)

    @Test fun cpuReferenceAlongsideEdgeTpuIsExcluded() {
        assertNull(NnapiGate.blocker(listOf(cpu, tpu)))
        assertNull(NnapiGate.blocker(listOf(tpu)))
    }

    @Test fun aGpuCannotBeMistakenForTensorNpu() {
        assertNotNull(NnapiGate.blocker(listOf(cpu, tpu, NnapiDevice("gpu", 3, "1", 31))))
        assertNotNull(NnapiGate.blocker(listOf(cpu, NnapiDevice("google-edgetpu", 3, "1", 31))))
    }

    @Test fun absentOrUnidentifiedAcceleratorsFailClosed() {
        assertNotNull(NnapiGate.blocker(emptyList()))
        assertNotNull(NnapiGate.blocker(listOf(cpu)))
        assertNotNull(NnapiGate.blocker(listOf(NnapiDevice("unknown", 4, "1", 31))))
        assertNotNull(NnapiGate.blocker(listOf(NnapiDevice("google-edgetpu", 0, "1", 31))))
    }
}
