package com.focx.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import com.focx.BuildConfig
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min


/**
 * @author chenjim me@h89.cn
 * @description Can print current thread name, location in code and other information
 * @date 2016/9/26.
 */
object Log {
    /**
     * Android's max limit for a log entry is ~4076 bytes,
     * so 4000 bytes is used as chunk size since default charset
     * is UTF-8
     */
    private const val CHUNK_SIZE = 4000


    /**
     * Log output level
     * [Log.ASSERT]>[Log.ERROR]>[Log.WARN]
     * >[Log.INFO]>[Log.DEBUG]>[Log.VERBOSE]
     */
    private var logLevel = Log.VERBOSE

    /**
     * Not further encapsulated, is 4
     * If further encapsulated, this value needs to be changed
     */
    private const val STACK_INDEX = 4

    /**
     * Single file size limit
     */
    private var logFileMaxLen = 5 * 1024 * 1024L

    var logDir: File? = null
        private set

    private const val CUR_LOG_NAME = "log1.txt"
    private const val LAST_LOG_NAME = "log2.txt"
    private val dateFormat by lazy { SimpleDateFormat("yyMMdd_HHmmss_SSS", Locale.US) }

    private val logHandler: Handler

    private var PRE = "LOG_"

    init {
        val handlerThread = HandlerThread("Logger")
        handlerThread.start()
        logHandler = Handler(handlerThread.getLooper())
    }

    /**
     * Initialization, not required
     * Required when need to write to log file
     * Log file location '/sdcard/Android/data/com.xxx.xxx/files/log/'
     *
     * @param writeFileContext null, do not write log
     * non-null, write log, used to get app data storage directory,
     * no permission required [Context.getExternalFilesDir]
     * Auth chenjim me@h89.cn
     * @param level            default value [.logLevel]
     */
    @JvmStatic
    fun init(writeFileContext: Context?, level: Int = Log.VERBOSE) {
        writeFileContext?.let {
            val path = File(it.getExternalFilesDir(null), "log")
            if (!path.exists()) {
                path.mkdirs()
            }
            logDir = path
            PRE = it.packageName.takeLast(4).uppercase() + "_"
            d("write log to dir: ${logDir!!.path}")
        }
        logLevel = level
    }

    /**
     * Configure log output level, default all [.logLevel]
     *
     * @param level
     */
    @JvmStatic
    fun init(level: Int = Log.VERBOSE) {
        init(null, level)
    }

    fun setLogFileMaxLen(len: Long) {
        logFileMaxLen = len
    }

    private fun objectToString(obj: Any?): String {
        return when (obj) {
            null -> "null"
            is Array<*> -> obj.contentDeepToString()
            is MutableList<*> -> obj.toTypedArray().contentDeepToString()
            else -> obj.toString()
        }
    }

    @JvmStatic
    fun d(obj: Any?) = log(Log.DEBUG, null, objectToString(obj))

    @JvmStatic
    fun d(tag: String?, message: String?) = log(Log.DEBUG, tag, message)

    @JvmStatic
    fun e(obj: Any?) = log(Log.ERROR, null, objectToString(obj))

    @JvmStatic
    fun e(tag: String?, message: String?) = log(Log.ERROR, tag, message)

    @JvmStatic
    fun e(tag: String?, e: Exception?) = log(Log.ERROR, tag, e?.toString() ?: "No message/exception is set")

    @JvmStatic
    fun e(tag: String?, message: String?, e: Exception) = log(Log.ERROR, tag, "$message: $e")

    @JvmStatic
    fun w(obj: Any?) = log(Log.WARN, null, objectToString(obj))

    @JvmStatic
    fun w(tag: String?, message: String?) = log(Log.WARN, tag, message)

    @JvmStatic
    fun i(obj: Any?) = log(Log.INFO, null, objectToString(obj))

    @JvmStatic
    fun i(tag: String?, message: String?) = log(Log.INFO, tag, message)

    @JvmStatic
    fun v(message: String?) = log(Log.VERBOSE, null, message)

    @JvmStatic
    fun v(tag: String?, message: String?) = log(Log.VERBOSE, tag, message)

    private fun log(logType: Int, tag: String?, message: String?) {
        if (logType < logLevel) return

        val curThread = Thread.currentThread()
        val element = curThread.getStackTrace()[STACK_INDEX]

        logHandler.post(Runnable { log(element, curThread, logType, tag, message) })
    }

    private fun log(
        element: StackTraceElement, thread: Thread,
        logType: Int, tag: String?, message: String?
    ) {
        val finalTag = PRE + (tag ?: "N")
        val builder = StringBuilder().apply {
            append("[").append(thread.id).append("],")
            if (BuildConfig.DEBUG) {
                append(thread.name).append(",(").append(element.fileName).append(":").append(element.lineNumber)
                    .append("),")
            }
        }
        val finalMessage = message ?: "null"
        val bytes = finalMessage.toByteArray()
        val length = bytes.size
        var i = 0
        while (i < length) {
            val count = min(length - i, CHUNK_SIZE)
            val content = String(bytes, i, count)
            logChunk(logType, finalTag, builder.toString() + content)
            i += CHUNK_SIZE
        }
    }

    private fun logChunk(logType: Int, tag: String?, chunk: String) {
        when (logType) {
            Log.ERROR -> Log.e(tag, chunk)
            Log.WARN -> Log.w(tag, chunk)
            Log.INFO -> Log.i(tag, chunk)
            Log.DEBUG -> Log.d(tag, chunk)
            Log.VERBOSE -> Log.v(tag, chunk)
            else -> {}
        }
        writeLogToFile(logType, tag, chunk)
    }

    @SuppressLint("SimpleDateFormat")
    @Synchronized
    private fun writeLogToFile(logType: Int, tag: String?, msg: String?) {
        logDir?.let {
            val data = StringBuilder().apply {
                append("[pid:").append(Process.myPid()).append("][")
                append(dateFormat.format(Date())).append(": ")
                append(getTypeString(logType)).append("/").append(tag).append("] ")
                append(msg).append("\r\n")
            }.toString()
            logHandler.post { doWriteDisk(data) }
        }
    }

    private fun doWriteDisk(msg: String?) {
        val curFile = File(logDir, CUR_LOG_NAME)
        val oldFile = File(logDir, LAST_LOG_NAME)
        if (curFile.length() > logFileMaxLen && !curFile.renameTo(oldFile)) {
            return
        }

        try {
            FileWriter(curFile, true).use { writer ->
                writer.write(msg)
                writer.flush()
            }
        } catch (exception: IOException) {
            exception.printStackTrace()
        }
    }

    private fun readLogFile(file: File, readLen: Long): StringBuilder {
        val data = StringBuilder()
        try {
            BufferedReader(FileReader(file)).use { reader ->
                val skip = if (file.length() > readLen) file.length() - readLen else 0
                reader.skip(skip)
                val length = if (readLen > file.length()) file.length() else readLen
                while (data.length < length) {
                    data.append(reader.readLine()).append("\r\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return data
    }


    /**
     * Read the last segment of logger
     */
    @JvmStatic
    fun getLastLogger(maxSize: Long): String {
        val data = StringBuilder()
        val curLogFile: File = File(logDir, CUR_LOG_NAME)
        if (!curLogFile.exists()) {
            return data.toString()
        }

        // Whether need to read the previous file
        if (curLogFile.length() < maxSize) {
            val lastLogFile = File(logDir, LAST_LOG_NAME)
            val lastLogData = readLogFile(lastLogFile, maxSize - curLogFile.length())
            data.append(lastLogData)
        }

        val curLogData = readLogFile(curLogFile, maxSize)
        data.append(curLogData)
        return data.toString()
    }

    @JvmStatic
    val stackTrace: String
        /**
         * @return filename + line number + function name at call site
         */
        get() {
            val trace = Thread.currentThread().getStackTrace()
            val element = trace[STACK_INDEX - 1]
            val builder = StringBuilder()
            builder.append("(")
                .append(element.fileName)
                .append(":")
                .append(element.lineNumber)
                .append("),")
                .append(element.methodName)
                .append("()")

            return builder.toString()
        }


    private fun getTypeString(logType: Int): String {
        return when (logType) {
            Log.ERROR -> "E"
            Log.WARN -> "W"
            Log.INFO -> "I"
            Log.DEBUG -> "D"
            Log.VERBOSE -> "V"
            else -> "D"
        }
    }


}
