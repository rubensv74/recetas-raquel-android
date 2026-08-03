package com.rmm.recetasraquel.domain.repository

interface DemoDataController {
    suspend fun load(): Result<Unit>
    suspend fun remove(): Result<Unit>
}
