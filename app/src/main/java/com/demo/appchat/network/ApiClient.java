package com.demo.appchat.network;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;

    public static Retrofit getclient(){
        if(retrofit == null){
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://fcm/googleapis/fcm/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return  retrofit;
    }
}
