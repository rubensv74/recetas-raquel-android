package com.rmm.recetasraquel.util

import java.util.UUID

fun interface IdGenerator {
    fun newId(): String
}

fun interface TimeProvider {
    fun nowEpochMillis(): Long
}

class UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}

class SystemTimeProvider : TimeProvider {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
