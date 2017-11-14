package com.example.slingshot;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Created by burha on 9/14/2017.
 */

public class Uploader {
    interface Service {
        @Multipart
        @POST("/upload")
        Call<ResponseBody> postImage(@Part MultipartBody.Part image);
    }
    public void sendImage(String path, String ip){
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

// Change base URL to your upload server URL.
        Service service = new Retrofit.Builder().baseUrl("http://" + ip.trim() + ":8085").client(client).build().create(Service.class);
        Log.i(MainActivity.TAG,"from uploader: " + ip);
        Log.i(MainActivity.TAG, "connecting to : " + "http://" + ip.trim() + ":8085");




        File file = new File(path);

        RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image",file.getName(), reqFile);
        retrofit2.Call<okhttp3.ResponseBody> req = service.postImage(body);
        req.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                //Log.i(MainActivity.TAG,response.message());
                try {
                    Log.i(MainActivity.TAG,response.body().string());

                }  catch (IOException | NullPointerException  e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(MainActivity.TAG,"okhttps " + t.getMessage());
            }
        });
    }
}
