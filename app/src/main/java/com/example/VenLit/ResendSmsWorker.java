package com.example.VenLit;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.List;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ResendSmsWorker extends Worker {

    private static final String BASE_URL = "https://backuptrack.salumtransports.co.tz/";

    public ResendSmsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SmsQueueManager smsQueueManager = new SmsQueueManager(getApplicationContext());

        List<SmsData> queuedSms = smsQueueManager.getQueuedSms();
        if (queuedSms.isEmpty()) {
            return Result.success();
        }

        Gson gson = new GsonBuilder().create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        SmsApi api = retrofit.create(SmsApi.class);

        boolean allSent = true;

        for (SmsData smsData : queuedSms) {
            try {
                Response<SmsResponse> response = api.sendSms(smsData).execute();
                if (!response.isSuccessful()) {
                    allSent = false;
                }
            } catch (IOException e) {
                allSent = false;
                e.printStackTrace();
            }
        }

        if (allSent) {
            smsQueueManager.clearQueue();
            return Result.success();
        } else {
            return Result.retry();
        }
    }
}