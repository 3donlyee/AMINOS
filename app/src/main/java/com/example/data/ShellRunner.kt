package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object ShellRunner {

    fun checkRootAvailable(): Boolean {
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
        return paths.any { File(it).exists() }
    }

    suspend fun executeCommand(command: String, asRoot: Boolean = false): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            try {
                val shell = if (asRoot && checkRootAvailable()) "su" else "sh"
                val process = ProcessBuilder(shell, "-c", command)
                    .redirectErrorStream(true)
                    .start()

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                val exitCode = process.waitFor()
                val result = output.toString().trim()
                Pair(exitCode, if (result.isEmpty()) "(Command executed with no output)" else result)
            } catch (e: Exception) {
                Pair(-1, "Error: ${e.localizedMessage ?: "Unknown execution error"}")
            }
        }
}
