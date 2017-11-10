package com.example.slingshot;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.Tag;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.zip.Inflater;

public class Welcome extends AppCompatActivity implements SensorEventListener{
    private MqttAndroidClient client;
    private SensorManager mSensorManager;
    private float azimuth;
    static final float ALPHA = 0.25f;
    private float gravity[];
    private float magnetic[]; //for magnetic rotational data
    private float accels[] = new float[3];
    private float mags[] = new float[3];
    private float[] values = new float[3];
    private SharedPreferences sp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        Button start = (Button) findViewById(R.id.start);
        sp = getApplicationContext().getSharedPreferences("data", MODE_PRIVATE);
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Welcome.this, Data.class));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflaterMenu = getMenuInflater();
        inflaterMenu.inflate(R.menu.config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.calibrate){

           AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Press okay after putting the phone in the center of the screen")
                    .setPositiveButton("okay", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            calc();
                        }
                    });
             builder.create().show();
        }
        else if(item.getItemId() == R.id.clear){

            String topic = sp.getString("topic",null);
            String payload = topic + "^remove^";
//            Log.i(MainActivity.TAG,topic);
            byte[] encodedPayload = new byte[0];
            try {
                encodedPayload = payload.getBytes("UTF-8");
                MqttMessage message = new MqttMessage(encodedPayload);
                if(topic!=null)
                    client.publish("Sling", message);
                else
                    Toast.makeText(getApplicationContext(),"please add topic first",Toast.LENGTH_LONG).show();

            } catch (UnsupportedEncodingException | MqttException | NullPointerException e) {
                Log.e(MainActivity.TAG,e.getMessage());
            }
        }
        else if(item.getItemId() == R.id.topic){
            set(0);
        }
        else if(item.getItemId() == R.id.mip){
            set(1);

        }
        return true;
    }
    public void set(final int n){
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
       final View layout = inflater.inflate(R.layout.namedialog,
                (ViewGroup) findViewById(R.id.nameLayout));
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText text =  layout.findViewById(R.id.nameDialog);
        builder.setView(layout);

        if(n ==0) {
            builder.setTitle("Enter Topic");
            text.setHint(sp.getString("topic","topic"));
        }
        else if(n==1) {
            builder.setTitle("Enter Mqttip");
            text.setHint(sp.getString("mip","ip"));
        }
        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.i(MainActivity.TAG,"OKAY Pressed");

                String converted = text.getText().toString();
                if(converted.length() != 0){
                    Log.i(MainActivity.TAG,"\nname = " + text);
                    SharedPreferences.Editor editor = sp.edit();
                    if(n==0)
                        editor.putString("topic",converted);
                    else if(n==1)
                        editor.putString("mip", converted);

                    editor.commit();

                }

            }
        });

        AlertDialog nameDialog;
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {


            }
        });
        nameDialog = builder.create();
        nameDialog.show();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mags = lowPass(sensorEvent.values.clone(),mags);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accels = lowPass(sensorEvent.values.clone(),accels);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    public void calc(){
        String topic = sp.getString("topic",null);
       // Log.i(MainActivity.TAG,topic);
        gravity = new float[9];
        magnetic = new float[9];
        SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
        float[] outGravity = new float[9];
        SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
        SensorManager.getOrientation(outGravity, values);
        azimuth = ((float) ((values[0] *180)/Math.PI)+180);
        String payload = topic + "^calibrate^"+ (int)  azimuth;
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            if(topic!=null)
                client.publish("Sling", message);
            else
                Toast.makeText(getApplicationContext(),"please add topic first",Toast.LENGTH_LONG).show();
        } catch (UnsupportedEncodingException | MqttException | NullPointerException e) {
            Log.e(MainActivity.TAG,e.getMessage());
        }

    }
    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(this);
        super.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        String mip = sp.getString("mip",null);
        if(mip != null){
            String clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(this.getApplicationContext(), "tcp://"+mip+":1883",
                    clientId);
            try {
                IMqttToken token = client.connect();
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // We are connected
                        Log.d(MainActivity.TAG, "onSuccess");

                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        // Something went wrong e.g. connection timeout or firewall problems
                        Log.e(MainActivity.TAG, "onFailure");
                        Log.e(MainActivity.TAG, exception.getMessage());


                    }
                });
            } catch (MqttException | NullPointerException e) {
                Log.e(MainActivity.TAG,"mqtt" + e.getMessage());
            }
        }
        else
            Toast.makeText(getApplicationContext(),"set mqtt ip",Toast.LENGTH_LONG).show();
        mSensorManager.registerListener( this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL,SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }
}
