package com.example.VenLit;


import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;


public class CheckServiceJobService extends JobService {
    private static final int JOB_ID = 1;
    private static final long MAX_INACTIVE_DURATION = 15 * 60 * 1000;

    @Override
    public boolean onStartJob(JobParameters params) {
        SharedPreferences prefs = getSharedPreferences(SmsService.PREFS_NAME, MODE_PRIVATE);
        long lastActive = prefs.getLong(SmsService.KEY_LAST_ACTIVE, 0);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastActive > MAX_INACTIVE_DURATION) {
            // Restart service
            Intent serviceIntent = new Intent(this, SmsService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }

        jobFinished(params, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public static void scheduleJob(Context context) {
        ComponentName component = new ComponentName(context, CheckServiceJobService.class);
        JobInfo.Builder jobBuilder = new JobInfo.Builder(JOB_ID, component)
                .setPeriodic(4 * 60 * 1000) // 15 minutes
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) {
            scheduler.schedule(jobBuilder.build());
        }
    }
}