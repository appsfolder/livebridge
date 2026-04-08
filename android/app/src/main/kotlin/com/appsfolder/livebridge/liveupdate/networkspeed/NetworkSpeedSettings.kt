package com.appsfolder.livebridge.liveupdate.networkspeed

data class NetworkSpeedSettings(
    val enabled: Boolean = false,
    val displayMode: NetworkSpeedDisplayMode = NetworkSpeedDisplayMode.TOTAL,
    val uploadPrefix: String = DEFAULT_UPLOAD_PREFIX,
    val downloadPrefix: String = DEFAULT_DOWNLOAD_PREFIX,
    val prioritizeUploadSpeed: Boolean = true,
    val chipBackgroundDisabled: Boolean = false,
    val unitSelection: String = NetworkSpeedUnit.AUTO.id,
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "enabled" to enabled,
            "display_mode" to displayMode.id,
            "upload_prefix" to uploadPrefix,
            "download_prefix" to downloadPrefix,
            "prioritize_upload_speed" to prioritizeUploadSpeed,
            "chip_background_disabled" to chipBackgroundDisabled,
            "unit" to unitSelection,
        )
    }

    companion object {
        const val DEFAULT_UPLOAD_PREFIX = "\u25B2 "
        const val DEFAULT_DOWNLOAD_PREFIX = "\u25BC "
    }
}

enum class NetworkSpeedDisplayMode(val id: String) {
    TOTAL("total"),
    UPLOAD_ONLY("upload_only"),
    DOWNLOAD_ONLY("download_only");

    companion object {
        fun from(raw: String?): NetworkSpeedDisplayMode {
            return entries.firstOrNull { it.id == raw } ?: TOTAL
        }
    }
}

enum class NetworkSpeedUnit(val id: String) {
    AUTO("auto"),
    BYTES("bytes"),
    KILOBYTES("kilobytes"),
    MEGABYTES("megabytes"),
    GIGABYTES("gigabytes");

    companion object {
        fun parseSelection(raw: String?): Set<NetworkSpeedUnit> {
            return raw.orEmpty()
                .split(",")
                .mapNotNull { token ->
                    entries.firstOrNull { it.id == token.trim() }
                }
                .toSet()
        }

        fun normalizeSelection(raw: String?): String {
            if (raw == null) {
                return AUTO.id
            }

            val selected = parseSelection(raw)
            if (selected.isEmpty()) {
                return AUTO.id
            }
            if (selected.contains(AUTO)) {
                return AUTO.id
            }

            return entries
                .filter { it != AUTO && selected.contains(it) }
                .joinToString(",") { it.id }
        }
    }
}
