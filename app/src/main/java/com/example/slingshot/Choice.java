package com.example.slingshot;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Choice extends AppCompatActivity {

    private ListView list;
    private ArrayAdapter<String> choices;
    private final static String stringOfChoices [] = {"Take picture only"
            , "Take picture with your name"
            , "Take picture with your signature"
            , "Only name"
            , "Only signature"} ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);
        list = (ListView) findViewById(R.id.list);
        choices = new ArrayAdapter<String>(this,R.layout.list_choices,stringOfChoices);
        list.setAdapter(choices);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(i == 4)
                    startActivity(new Intent(Choice.this,Signature.class));
                else
                    startActivity(new Intent(Choice.this,Camera.class));
            }
        });
    }
}
