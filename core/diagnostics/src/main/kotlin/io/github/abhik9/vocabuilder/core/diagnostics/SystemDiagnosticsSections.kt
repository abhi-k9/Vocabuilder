package io.github.abhik9.vocabuilder.core.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.vocabuilder.core.common.diagnostics.DiagnosticsSection
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

internal class AppDiagnosticsSection @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DiagnosticsSection {
    override val title = "App"

    override suspend fun describe(): List<String> {
        val info = context.packageInfo()
        return listOf(
            "Version: ${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)}), ${context.packageName}",
            "Debuggable: ${context.isDebuggable}",
            "Installed: ${info.firstInstallTime.asDateTime()}, updated: ${info.lastUpdateTime.asDateTime()}",
        )
    }

    @Suppress("DEPRECATION")
    private fun Context.packageInfo(): PackageInfo = if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        packageManager.getPackageInfo(packageName, 0)
    }
}

internal class DeviceDiagnosticsSection @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DiagnosticsSection {
    override val title = "Device"

    override suspend fun describe(): List<String> {
        val configuration = context.resources.configuration
        val metrics = context.resources.displayMetrics
        val nightMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return listOf(
            "Device: ${Build.MANUFACTURER} ${Build.MODEL}, build ${Build.DISPLAY}",
            "Android: ${Build.VERSION.RELEASE} (API $SDK_INT)",
            "Locale: ${Locale.getDefault().toLanguageTag()}, time zone: ${ZoneId.systemDefault()}",
            "Screen: ${metrics.widthPixels}x${metrics.heightPixels} px, density ${metrics.density}, " +
                "font scale ${configuration.fontScale}, dark: $nightMode",
        )
    }
}

/** Why Android ended the app's recent processes, e.g. a crash, low memory or the user. */
internal class ProcessExitsDiagnosticsSection @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DiagnosticsSection {
    override val title = "Process exits"

    override suspend fun describe(): List<String> = if (SDK_INT >= Build.VERSION_CODES.R) {
        exits().ifEmpty { listOf("None") }
    } else {
        listOf("Unavailable before Android 11")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun exits(): List<String> = context.getSystemService(ActivityManager::class.java)
        .getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXITS)
        .map { exit ->
            val reason = EXIT_REASONS.getOrNull(exit.reason) ?: "reason ${exit.reason}"
            "${exit.timestamp.asDateTime()} $reason, status ${exit.status}, importance ${exit.importance}: " +
                exit.description.orEmpty()
        }

    private companion object {
        const val MAX_EXITS = 10

        /** ApplicationExitInfo reasons by value, since some constants are newer than the minimum SDK. */
        val EXIT_REASONS = listOf(
            "unknown", "exit self", "signaled", "low memory", "crash", "native crash", "ANR",
            "initialization failure", "permission change", "excessive resource usage", "user requested",
            "user stopped", "dependency died", "other", "freezer", "package state change", "package updated",
        )
    }
}

private fun Long.asDateTime(): OffsetDateTime =
    OffsetDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS)
