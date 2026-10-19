package de.simon.dankelmann.bluetoothlespam.Widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.R

/**
 * Home-screen widget with a single start/stop button that mirrors [de.simon.dankelmann.
 * bluetoothlespam.Services.SpamTileService]. Toggles only the already-loaded collection via
 * AdvertisementSetQueueHandler.toggle(); it never changes WHAT is advertised. Ease-of-use feature
 * ported in spirit from the ars2nb fork.
 */
class SpamWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { renderWidget(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            // onReceive runs on the main thread, which is what toggle() requires (it touches
            // callbacks that update Compose/LiveData state and may show a Toast).
            (context.applicationContext as BleSpamApplication).queueHandler.toggle(context)
            requestUpdateAll(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "de.simon.dankelmann.bluetoothlespam.action.WIDGET_TOGGLE"

        /** Re-render every placed widget to reflect the current active/inactive state. */
        fun requestUpdateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, SpamWidgetProvider::class.java))
            ids.forEach { renderWidget(context, manager, it) }
        }

        private fun renderWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val active = (context.applicationContext as BleSpamApplication).queueHandler.isActive()
            val views = RemoteViews(context.packageName, R.layout.widget_spam)
            views.setTextViewText(
                R.id.widget_status,
                context.getString(if (active) R.string.widget_spam_on else R.string.widget_spam_off),
            )
            views.setImageViewResource(
                R.id.widget_button,
                if (active) R.drawable.pause else R.drawable.play_arrow,
            )

            val toggleIntent = Intent(context, SpamWidgetProvider::class.java).apply { action = ACTION_TOGGLE }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            manager.updateAppWidget(widgetId, views)
        }
    }
}
