package com.example.picshot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Data extends AppCompatActivity {

    EditText nameText;
    EditText pledgeText;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        nameText = (EditText) findViewById(R.id.name);
        pledgeText = (EditText) findViewById(R.id.pledge);
         sp = getApplicationContext().getSharedPreferences("data", MODE_PRIVATE);
        final SharedPreferences.Editor editor = sp.edit();
        final Button done = (Button) findViewById(R.id.done);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = nameText.getText().toString();
                String pledge = pledgeText.getText().toString();
                if (name.isEmpty())
                    nameText.setError("cannot be empty");
                else if (pledge.isEmpty())
                    pledgeText.setError("cannot be empty");
                else {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("name", name);
                    editor.putString("pledge", pledge);
                    editor.commit();
                    startActivity(new Intent(Data.this, Shot.class));
                }
            }
        });
    }
}
