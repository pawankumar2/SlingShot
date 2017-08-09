package com.example.slingshot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class Shot extends AppCompatActivity {
    private SharedPreferences pref;
    private String signature;
    private String name;
    private String image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shot);
        pref = getApplicationContext().getSharedPreferences("data", Context.MODE_PRIVATE);
        signature = pref.getString("signature",null);
        name = pref.getString("name",null);
        image = pref.getString("image", null);
        Log.i(MainActivity.TAG, "Image: " + image
                + "\n Signature: " + signature
                + "\nName: " + name);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Shot.this,Camera.class));
                finish();
            }
        },2000);
    }
}
