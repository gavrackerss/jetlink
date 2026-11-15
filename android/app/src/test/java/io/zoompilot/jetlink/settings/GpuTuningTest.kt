package io.zoompilot.jetlink.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The GPU A/B experiment must always default to the stock fp16 OpenCL mode. */
class GpuTuningTest {
    @Test fun safeFallback() {
        assertEquals(GpuTuning.Stock, GpuTuning.of(null))
        assertEquals(GpuTuning.Stock, GpuTuning.of(""))
        assertEquals(GpuTuning.Stock, GpuTuning.of("unknown"))
    }

    @Test fun roundTripAndUniqueKeys() {
        val values = GpuTuning.entries
        assertEquals(4, values.size)
        assertEquals("default", GpuTuning.Stock.key)
        assertEquals(values.size, values.map { it.key }.distinct().size)
        values.forEach {
            assertEquals(it, GpuTuning.of(it.key))
            assertTrue(it.title.isNotBlank())
        }
    }
}
