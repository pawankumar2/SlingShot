package com.example.slingshot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;

public class Data extends AppCompatActivity {

    EditText nameText;
    EditText pledgeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        nameText = (EditText) findViewById(R.id.name);
        pledgeText = (EditText) findViewById(R.id.pledge);
        String name = nameText.getText().toString();
        String pledge = pledgeText.getText().toString();
    }
}
