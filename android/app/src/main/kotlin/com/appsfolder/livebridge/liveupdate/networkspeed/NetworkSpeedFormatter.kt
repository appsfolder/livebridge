package com.appsfolder.livebridge.liveupdate.networkspeed

import java.util.Locale

object NetworkSpeedFormatter {
    fun formatChipText(
        bytesPerSecond: Long,
        rawUnit: String?,
    ): String {
        return when (resolveDisplayUnit(rawUnit, bytesPerSecond)) {
            NetworkSpeedUnit.BYTES -> "${formatFixedValue(bytesPerSecond.toDouble())}B/s"
            NetworkSpeedUnit.KILOBYTES -> "${formatFixedValue(bytesPerSecond / 1024.0)}K/s"
            NetworkSpeedUnit.MEGABYTES -> "${formatFixedValue(bytesPerSecond / 1048576.0)}M/s"
            NetworkSpeedUnit.GIGABYTES -> "${formatFixedValue(bytesPerSecond / 1073741824.0)}G/s"
            NetworkSpeedUnit.AUTO -> {
                if (bytesPerSecond < 1024) {
                    return "${bytesPerSecond}B/s"
                }
                val kb = bytesPerSecond / 1024.0
                if (kb < 1000) {
                    return "${"%.0f".format(Locale.getDefault(), kb)}K/s"
                }
                val mb = kb / 1024.0
                if (mb < 1000) {
                    return if (mb < 100) {
                        "${"%.1f".format(Locale.getDefault(), mb)}M/s"
                    } else {
                        "${"%.0f".format(Locale.getDefault(), mb)}M/s"
                    }
                }
                val gb = mb / 1024.0
                "${"%.1f".format(Locale.getDefault(), gb)}G/s"
            }
        }
    }

    fun formatSpeedText(
        bytesPerSecond: Long,
        rawUnit: String?,
    ): Pair<String, String> {
        return when (resolveDisplayUnit(rawUnit, bytesPerSecond)) {
            NetworkSpeedUnit.BYTES -> formatFixedValue(bytesPerSecond.toDouble()) to "B/s"
            NetworkSpeedUnit.KILOBYTES -> formatFixedValue(bytesPerSecond / 1024.0) to "KB/s"
            NetworkSpeedUnit.MEGABYTES -> formatFixedValue(bytesPerSecond / 1048576.0) to "MB/s"
            NetworkSpeedUnit.GIGABYTES -> formatFixedValue(bytesPerSecond / 1073741824.0) to "GB/s"
            NetworkSpeedUnit.AUTO -> {
                if (bytesPerSecond < 1024) {
                    return bytesPerSecond.toString() to "B/s"
                }
                val kb = bytesPerSecond / 1024.0
                if (kb < 1000) {
                    return "%.0f".format(Locale.getDefault(), kb) to "KB/s"
                }
                val mb = kb / 1024.0
                if (mb < 1000) {
                    return if (mb < 10) {
                        "%.1f".format(Locale.getDefault(), mb) to "MB/s"
                    } else {
                        "%.0f".format(Locale.getDefault(), mb) to "MB/s"
                    }
                }
                val gb = mb / 1024.0
                "%.1f".format(Locale.getDefault(), gb) to "GB/s"
            }
        }
    }

    fun formatLine(
        bytesPerSecond: Long,
        rawUnit: String?,
    ): String {
        val (value, unit) = formatSpeedText(bytesPerSecond, rawUnit)
        return "$value$unit"
    }

    private fun resolveDisplayUnit(
        rawUnit: String?,
        bytesPerSecond: Long,
    ): NetworkSpeedUnit {
        val selectedUnits = NetworkSpeedUnit.parseSelection(rawUnit)
        if (selectedUnits.isEmpty() || selectedUnits.contains(NetworkSpeedUnit.AUTO)) {
            return NetworkSpeedUnit.AUTO
        }

        val fixedUnits = selectedUnits
            .filter { it != NetworkSpeedUnit.AUTO }
            .sortedByDescending { it.ordinal }
        var bestUnit = fixedUnits.last()
        for (unit in fixedUnits) {
            val threshold =
                when (unit) {
                    NetworkSpeedUnit.GIGABYTES -> 1000L * 1024L * 1024L
                    NetworkSpeedUnit.MEGABYTES -> 1000L * 1024L
                    NetworkSpeedUnit.KILOBYTES -> 1024L
                    NetworkSpeedUnit.BYTES,
                    NetworkSpeedUnit.AUTO -> 0L
                }
            if (bytesPerSecond >= threshold) {
                bestUnit = unit
                break
            }
        }
        return bestUnit
    }

    private fun formatFixedValue(value: Double): String {
        return when {
            value < 10 -> "%.2f".format(Locale.getDefault(), value)
            value < 100 -> "%.1f".format(Locale.getDefault(), value)
            else -> "%.0f".format(Locale.getDefault(), value)
        }
    }
}
