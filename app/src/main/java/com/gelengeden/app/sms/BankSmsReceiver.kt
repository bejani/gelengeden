package com.gelengeden.app.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gelengeden.app.GelengedenApp
import com.gelengeden.app.R
import com.gelengeden.app.data.PendingBankSms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives system SMS broadcasts only after the user has opted in and granted RECEIVE_SMS.
 * It never creates a final transaction itself: it queues a local draft for user confirmation.
 */
class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        receiverScope.launch {
            try {
                val app = appContext as GelengedenApp
                val configuredSenders = app.repository.getAllBankSendersOnce()
                if (configuredSenders.isEmpty()) return@launch

                Telephony.Sms.Intents.getMessagesFromIntent(intent).forEach { message ->
                    val senderAddress = message.displayOriginatingAddress.orEmpty()
                    val sender = configuredSenders.firstOrNull {
                        BankSmsParser.senderMatches(it.address, senderAddress)
                    } ?: return@forEach

                    val body = message.messageBody.orEmpty()
                    val parsed = BankSmsParser.parse(body) ?: return@forEach
                    val receivedAt = message.timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()
                    val pendingSms = PendingBankSms(
                        senderAddress = senderAddress,
                        senderLabel = sender.label,
                        body = "",
                        receivedAt = receivedAt,
                        rawAmount = parsed.amount,
                        amountWasRial = sender.amountWasRial,
                        suggestedTitle = "",
                        suggestedType = parsed.type,
                        fingerprint = BankSmsParser.fingerprint(senderAddress, body, receivedAt)
                    )
                    if (app.repository.enqueuePendingBankSms(pendingSms)) {
                        BankSmsNotifier.show(appContext)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

private object BankSmsNotifier {
    private const val CHANNEL_ID = "bank_sms_review"
    private const val NOTIFICATION_ID = 4101

    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.sms_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(context.getString(R.string.sms_notification_title))
                .setContentText(context.getString(R.string.sms_notification_body))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()
        )
    }
}
