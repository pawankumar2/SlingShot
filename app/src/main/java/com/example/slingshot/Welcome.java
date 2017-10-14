package com.example.slingshot;

import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        Button start = (Button) findViewById(R.id.start);
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
            String payload = "remove^";
            String topic = "Sling";
            byte[] encodedPayload = new byte[0];
            try {
                encodedPayload = payload.getBytes("UTF-8");
                MqttMessage message = new MqttMessage(encodedPayload);
                client.publish(topic, message);
            } catch (UnsupportedEncodingException | MqttException | NullPointerException e) {
                Log.e(MainActivity.TAG,e.getMessage());
            }
        }
        return true;
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
        gravity = new float[9];
        magnetic = new float[9];
        SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
        float[] outGravity = new float[9];
        SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
        SensorManager.getOrientation(outGravity, values);
        azimuth = ((float) ((values[0] *180)/Math.PI)+180);
        String payload = "calibrate^"+ (int)  azimuth;
        String topic = "Sling";
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
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
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://172.16.0.51:1883",
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
        mSensorManager.registerListener( this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL,SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }
}
