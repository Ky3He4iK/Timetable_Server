package main.kotlin.com.ky3he4ik.timetable.server.util

import java.text.SimpleDateFormat
import java.util.*

internal object LOG {
    private const val logBoundaryOpen = "------LOG PART------"
    private const val logBoundaryClose = "____END LOG PART____"
    private const val esc = "\u001B"
    private const val bold = "[1;"
    private const val normal = "[0;"
    private const val endEsc = "m"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:SS_Z", Locale.ENGLISH)
    val now: String
        get() = dateFormat.format(Date(System.currentTimeMillis()))
    private val normalText: String
        get() = "$esc$normal${Color.BLACK.code}$endEsc"

    enum class LogLevel(val lvl: Int, val color: Color, val isBold: Boolean) {
        VERBOSE(0, Color.GRAY, false),
        DEBUG(3, Color.WHITE, true),
        INFO(6, Color.WHITE, true),
        WARNING(9, Color.MAGENTA, true),
        ERROR(12, Color.RED, true),
        SILENT(100, Color.RED, true)
    }

    enum class Color(val code: String) {
        RED("31"),
        MAGENTA("35"),
        WHITE("30"),
        GRAY("38"),
        BLACK("37")
    }

    fun e(tag: String, message: String?, e: Exception) {
        println("${LOG.toColoredText(logBoundaryOpen, LogLevel.ERROR.color, true)} ${LogLevel.ERROR.name}" +
                " at $now\n$tag: $message")
        e.printStackTrace()
        Thread.sleep(10)
        println("\n${LOG.toColoredText(logBoundaryClose, LogLevel.ERROR.color, true)}$normalText")
    }

    fun e(tag: String, message: Any) = write(tag, message, LogLevel.ERROR)

    fun w(tag: String, message: Any) = write(tag, message, LogLevel.WARNING)

    fun i(tag: String, message: Any) = write(tag, message, LogLevel.INFO)

    fun d(tag: String, message: Any) = write(tag, message, LogLevel.DEBUG)

    fun v(tag: String, message: Any) = write(tag, message, LogLevel.VERBOSE)

    fun extremelyImportant(tag: String, message: Any) = write(tag, message, LogLevel.SILENT)

    private fun write(tag: String, message: Any, level: LogLevel) {
        if (level.lvl >= Config.logLevel.lvl)
            println("${LOG.toColoredText(logBoundaryOpen, level.color, level.isBold)} ${level.name} at $now\n$tag: $message\n" +
                    "${LOG.toColoredText(logBoundaryClose, level.color, level.isBold)}$normalText")
    }

    private fun toColoredText(text: String, color: Color, isBold: Boolean): String
            = "$esc${if (isBold) bold else normal}${color.code}$endEsc$text"
}
