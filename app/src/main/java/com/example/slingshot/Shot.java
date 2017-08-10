package com.example.slingshot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

public class Shot extends AppCompatActivity {
    private SharedPreferences pref;
    private String signature;
    private String name;
    private String image;
    private String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shot);
        pref = getApplicationContext().getSharedPreferences("data", Context.MODE_PRIVATE);
        signature = pref.getString("signature",null);
        name = pref.getString("name",null);
        image = pref.getString("image", null);
        ip = pref.getString("ip",null);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

// Change base URL to your upload server URL.
        Service service = new Retrofit.Builder().baseUrl("http://192.168.0.18:80/image.php/").client(client).build().create(Service.class);



        File file = new File(image);

        RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("upload","hello", reqFile);
        RequestBody name2 = RequestBody.create(MediaType.parse("text/plain"), name);

        retrofit2.Call<okhttp3.ResponseBody> req = service.postImage(body, name2);
        req.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                //Log.i(MainActivity.TAG,response.message());
                Log.i(MainActivity.TAG,response.toString());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(MainActivity.TAG,t.getMessage());
            }
        });
    }
    interface Service {
        @Multipart
        @POST("/")
        Call<ResponseBody> postImage(@Part MultipartBody.Part image, @Part("name") RequestBody name);
    }
}
