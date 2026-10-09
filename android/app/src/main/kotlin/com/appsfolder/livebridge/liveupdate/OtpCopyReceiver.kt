package com.appsfolder.livebridge.liveupdate

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

class OtpCopyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_COPY_OTP) {
            return
        }

        val rawCode = intent.getStringExtra(EXTRA_OTP_CODE).orEmpty()
        val code = rawCode.filter(Char::isDigit)
        if (code.isBlank()) {
            return
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("OTP", code))
        Toast.makeText(context, copiedToastText(context), Toast.LENGTH_SHORT).show()
    }

    private fun copiedToastText(context: Context): String {
        return NativeAppStrings.text(context, "Code copied", "Код скопирован",
            "Código copiado", "Code kopiert", "Kod kopyalandı")
    }

    companion object {
        const val ACTION_COPY_OTP = "com.appsfolder.livebridge.action.COPY_OTP"
        const val EXTRA_OTP_CODE = "otp_code"
    }
}
