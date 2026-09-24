package com.example.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.security.ThiefGuardService

class QuickActivationWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_ARM -> {
                val serviceIntent = Intent(context, ThiefGuardService::class.java).apply {
                    action = ThiefGuardService.ACTION_ARM
                }
                context.startService(serviceIntent)
                refreshAllWidgets(context)
            }
            ACTION_WIDGET_STOP -> {
                val serviceIntent = Intent(context, ThiefGuardService::class.java).apply {
                    action = ThiefGuardService.ACTION_DISARM
                }
                context.startService(serviceIntent)
                refreshAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_ARM = "com.sajid.thiefhunterguard.ACTION_WIDGET_ARM"
        const val ACTION_WIDGET_STOP = "com.sajid.thiefhunterguard.ACTION_WIDGET_STOP"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.quick_widget_layout)

            // Open App on tap title / logo
            val mainIntent = Intent(context, MainActivity::class.java)
            val mainPendingIntent = PendingIntent.getActivity(
                context, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_logo, mainPendingIntent)

            // Arm Button Intent
            val armIntent = Intent(context, QuickActivationWidgetReceiver::class.java).apply {
                action = ACTION_WIDGET_ARM
            }
            val armPendingIntent = PendingIntent.getBroadcast(
                context, 1, armIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_arm, armPendingIntent)

            // Stop Button Intent
            val stopIntent = Intent(context, QuickActivationWidgetReceiver::class.java).apply {
                action = ACTION_WIDGET_STOP
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context, 2, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_stop, stopPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, QuickActivationWidgetReceiver::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
    }
}
