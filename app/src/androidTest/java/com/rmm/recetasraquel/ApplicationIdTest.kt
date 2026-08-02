package com.rmm.recetasraquel

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationIdTest {
    @Test
    fun applicationIdIsCorrect() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.rmm.recetasraquel", context.packageName)
    }
}
