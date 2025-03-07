package com.example.VenLit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        for (SmsMessage sms : messages) {
            String sender = sms.getOriginatingAddress();
            String messageBody = sms.getMessageBody();
            long timestamp = sms.getTimestampMillis();

            Log.d("SmsReceiver", "Received SMS: sender=" + sender + ", message=" + messageBody); // Log received message

            Intent serviceIntent = new Intent(context, SmsService.class);
            serviceIntent.setAction(SmsService.ACTION_SEND_SMS);
            serviceIntent.putExtra(SmsService.EXTRA_SENDER, sender);
            serviceIntent.putExtra(SmsService.EXTRA_TIMESTAMP, timestamp);
            serviceIntent.putExtra(SmsService.EXTRA_MESSAGE, messageBody);
            context.startService(serviceIntent);
            Log.d("SmsReceiver", "Started service for SMS processing"); // Log service start
        }
    }
}