package com.kakao.taxi.liveupdate

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.CircularProgressInfo
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.ProgressTextInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo
import java.util.Locale

internal object HyperBridgeAdapter {
    private const val TAG = "HyperBridgeAdapter"
    private const val PARAM_KEY = "miui.focus.param"
    private const val PIC_KEY_MAIN = "main"
    private const val MAX_HYPER_ACTIONS = 2
    private const val MAX_ACTION_TITLE_LENGTH = 22
    private const val BIG_ISLAND_TEXT_MAX_LENGTH = 72
    private const val HYPER_PROGRESS_COLOR = "#0F766E"

    fun apply(
        context: Context,
        builder: NotificationCompat.Builder,
        sourcePackageName: String,
        appName: String,
        title: String,
        content: String,
        ticker: String,
        progressPercent: Int?,
        largeIcon: Bitmap?,
        fallbackSmallIcon: IconCompat?,
        sourceActions: Array<Notification.Action>?
    ) {
        try {
            if (!HyperIslandNotification.isSupported(context)) {
                return
            }

            val normalizedTitle = title.ifBlank { appName }.ifBlank { sourcePackageName }.take(96)
            val normalizedContent = content.ifBlank { appName }.ifBlank { normalizedTitle }.take(220)
            val normalizedTicker = ticker.ifBlank { normalizedTitle }.take(40)
            val normalizedAppName = appName.take(48).ifBlank { null }
            val businessName = buildBusinessName(sourcePackageName)
            val progress = progressPercent?.coerceIn(0, 100)
            val hyperPicture = buildMainPicture(
                context = context,
                largeIcon = largeIcon,
                fallbackSmallIcon = fallbackSmallIcon
            )

            val hyperBuilder = HyperIslandNotification.Builder(
                context = context,
                businessName = businessName,
                ticker = normalizedTicker
            )
            val hyperActions = buildHyperActions(sourceActions)
            hyperActions.forEach(hyperBuilder::addAction)
            hyperPicture?.let(hyperBuilder::addPicture)

            hyperBuilder.setBaseInfo(
                title = normalizedTitle,
                content = normalizedContent,
                subTitle = normalizedAppName,
                pictureKey = hyperPicture?.key
            )

            if (hyperPicture != null) {
                hyperBuilder.setPicInfo(2, PIC_KEY_MAIN)
                if (progress != null) {
                    hyperBuilder
                        .setSmallIslandCircularProgress(
                            pictureKey = PIC_KEY_MAIN,
                            progress = progress,
                            color = HYPER_PROGRESS_COLOR,
                            isCCW = true
                        )
                        .setBigIslandInfo(
                            left = ImageTextInfoLeft(
                                type = 1,
                                picInfo = PicInfo(type = 1, pic = PIC_KEY_MAIN),
                                textInfo = TextInfo(title = normalizedTitle)
                            ),
                            progressText = ProgressTextInfo(
                                progressInfo = CircularProgressInfo(
                                    progress = progress,
                                    colorReach = HYPER_PROGRESS_COLOR,
                                    isCCW = true
                                ),
                                textInfo = TextInfo(title = "$progress%")
                            )
                        )
                } else {
                    hyperBuilder
                        .setSmallIsland(PIC_KEY_MAIN)
                        .setBigIslandInfo(
                            left = ImageTextInfoLeft(
                                type = 1,
                                picInfo = PicInfo(type = 1, pic = PIC_KEY_MAIN),
                                textInfo = TextInfo(title = normalizedTitle)
                            ),
                            centerText = TextInfo(
                                title = normalizedContent.take(BIG_ISLAND_TEXT_MAX_LENGTH)
                            )
                        )
                }
            }

            if (hyperActions.isNotEmpty()) {
                hyperBuilder.setTextButtons(*hyperActions.toTypedArray())
            }

            progress?.let(hyperBuilder::setProgressBar)
            hyperBuilder.setIslandConfig(priority = 2)

            val resourceBundle = hyperBuilder.buildResourceBundle()
            builder.addExtras(resourceBundle)
            builder.addExtras(
                Bundle().apply {
                    putString(PARAM_KEY, hyperBuilder.buildJsonParam())
                }
            )
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to apply HyperBridge payload: ${error.message}")
        }
    }

    private fun buildBusinessName(sourcePackageName: String): String {
        val normalized = sourcePackageName
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_.-]"), "_")
            .take(80)
        return "livebridge.$normalized"
    }

    private fun buildMainPicture(
        context: Context,
        largeIcon: Bitmap?,
        fallbackSmallIcon: IconCompat?
    ): HyperPicture? {
        largeIcon?.let { return HyperPicture(PIC_KEY_MAIN, it) }
        val fallbackBitmap = try {
            fallbackSmallIcon?.loadDrawable(context)?.let(::drawableToBitmap)
        } catch (_: Throwable) {
            null
        }
        return fallbackBitmap?.let { HyperPicture(PIC_KEY_MAIN, it) }
    }

    private fun buildHyperActions(sourceActions: Array<Notification.Action>?): List<HyperAction> {
        if (sourceActions.isNullOrEmpty()) {
            return emptyList()
        }
        val result = mutableListOf<HyperAction>()
        val seenKeys = HashSet<String>()
        var index = 0
        while (index < sourceActions.size && result.size < MAX_HYPER_ACTIONS) {
            val action = sourceActions[index]
            val pendingIntent = action.actionIntent
            val title = action.title?.toString()?.trim().orEmpty()
            val icon = action.getIcon()
            val hasTitle = title.isNotBlank()
            val dedupeKey = title.lowercase(Locale.ROOT)
            if (pendingIntent != null && hasTitle && seenKeys.add(dedupeKey)) {
                val actionIntentType = resolveActionIntentType(pendingIntent)
                val normalizedIcon = normalizeActionIcon(icon)
                result += HyperAction(
                    "src_${index + 1}",
                    title.take(MAX_ACTION_TITLE_LENGTH),
                    normalizedIcon,
                    pendingIntent,
                    actionIntentType,
                    null,
                    null,
                    null,
                    null,
                    false,
                    0,
                    null,
                    null,
                    false
                )
            }
            index += 1
        }
        return result
    }

    private fun normalizeActionIcon(icon: Icon?): Icon? {
        return icon?.takeIf {
            it.type == Icon.TYPE_BITMAP ||
                    it.type == Icon.TYPE_RESOURCE ||
                    it.type == Icon.TYPE_URI ||
                    it.type == Icon.TYPE_ADAPTIVE_BITMAP ||
                    it.type == Icon.TYPE_URI_ADAPTIVE_BITMAP
        }
    }

    private fun resolveActionIntentType(pendingIntent: PendingIntent): Int {
        return when {
            pendingIntent.isActivity -> 1
            pendingIntent.isBroadcast -> 2
            else -> 3
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is android.graphics.drawable.BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(512)
        val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(512)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
