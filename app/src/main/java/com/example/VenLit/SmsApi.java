package com.example.VenLit;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SmsApi {
    @POST("venlit.php")
    Call<SmsResponse> sendSms(@Body SmsData smsData);
}