package com.example.VenLit;

import android.content.Context;

import com.google.gson.Gson;
import com.example.VenLit.SmsData;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SmsQueueManager {
    private static final String QUEUE_FILE = "queued_sms.txt";
    private final Context context;

    public SmsQueueManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void enqueueSms(SmsData smsData) {
        try {
            FileOutputStream fos = context.openFileOutput(QUEUE_FILE, Context.MODE_APPEND);
            String json = new Gson().toJson(smsData);
            fos.write((json + "\n").getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<SmsData> getQueuedSms() {
        List<SmsData> queuedSms = new ArrayList<>();
        try {
            FileInputStream fis = context.openFileInput(QUEUE_FILE);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int read;
            while ((read = fis.read()) != -1) {
                bos.write(read);
            }
            fis.close();
            String[] lines = bos.toString().split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    SmsData smsData = new Gson().fromJson(line, SmsData.class);
                    queuedSms.add(smsData);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return queuedSms;
    }

    public void clearQueue() {
        try {
            FileOutputStream fos = context.openFileOutput(QUEUE_FILE, Context.MODE_PRIVATE);
            fos.write("".getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}