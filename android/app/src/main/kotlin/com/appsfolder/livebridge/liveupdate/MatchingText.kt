package com.appsfolder.livebridge.liveupdate

import java.util.Locale

// Locale.ROOT lowercases "İ" to "i" + U+0307, which breaks dictionary matching and shifts
// indices against the source text, so fold it to a plain "i" first.
internal fun String.lowercaseForMatching(): String {
    return replace('İ', 'i').lowercase(Locale.ROOT)
}
