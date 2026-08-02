package com.rmm.recetasraquel

import com.rmm.recetasraquel.ui.navigation.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectFoundationTest {
    @Test
    fun sprintZeroNavigationContainsOnlyActiveDestinations() {
        assertEquals(listOf(AppDestination.Home, AppDestination.Settings), AppDestination.entries)
    }
}
