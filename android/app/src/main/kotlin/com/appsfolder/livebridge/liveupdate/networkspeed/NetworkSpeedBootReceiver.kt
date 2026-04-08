package com.appsfolder.livebridge.liveupdate.networkspeed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NetworkSpeedBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        NetworkSpeedController.sync(context)
    }
}
