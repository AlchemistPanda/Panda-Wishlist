package com.panda.wishlist.core.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class LogLevel { INFO, ACTION, ALERT, WARN }

data class DebugLogEntry(
    val id: Long,
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

@Singleton
class DebugLogManager @Inject constructor() {
    private val _logs = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val logs: StateFlow<List<DebugLogEntry>> = _logs.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private var counter = 0L

    @Synchronized
    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        counter++
        val entry = DebugLogEntry(
            id = counter,
            timestamp = timeFormat.format(Date()),
            level = level,
            tag = tag,
            message = message
        )
        _logs.value = (_logs.value + entry).takeLast(250)
    }

    @Synchronized
    fun clear() {
        _logs.value = emptyList()
    }

    fun exportAsText(): String {
        return _logs.value.joinToString("\n") {
            "[${it.timestamp}] [${it.level.name}] ${it.tag}: ${it.message}"
        }
    }
}
