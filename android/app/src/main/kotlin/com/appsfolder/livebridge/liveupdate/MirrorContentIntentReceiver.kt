package com.appsfolder.livebridge.liveupdate

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class MirrorContentIntentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sourceKey = intent.getStringExtra(EXTRA_SOURCE_KEY)?.takeIf { it.isNotBlank() }
            ?: return
        val sourceIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_SOURCE_CONTENT_INTENT, PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_SOURCE_CONTENT_INTENT)
        }

        LiveUpdateNotifier.handleMirroredContentTap(context, sourceKey)
        try {
            sourceIntent?.send()
        } catch (error: PendingIntent.CanceledException) {
            Log.w(TAG, "Source content intent was cancelled: $sourceKey", error)
        }
    }

    companion object {
        const val EXTRA_SOURCE_KEY = "source_key"
        const val EXTRA_SOURCE_CONTENT_INTENT = "source_content_intent"
        private const val TAG = "MirrorContentIntent"
    }
}
