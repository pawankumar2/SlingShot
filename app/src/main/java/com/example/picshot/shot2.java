package com.example.picshot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class shot2 extends AppCompatActivity implements SensorEventListener {

    private SharedPreferences sp;
    private MqttAndroidClient client;
    private SensorManager mSensorManager;
    private float y;
    private String payload;
    private float SHAKE_THRESHOLD;
    private String topic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shot2);
        new Fullscreen(findViewById(R.id.shot2)).hideSystemUI();
        sp = getApplicationContext().getSharedPreferences("data",MODE_PRIVATE);
        String ip = sp.getString("mip",null);
        payload = sp.getString("payload",null);
        SHAKE_THRESHOLD =  Integer.parseInt(sp.getString("hold","-7"));
        topic = sp.getString("topic",null);
        if (ip == null) {
            Toast.makeText(getApplicationContext(), "set mqtt ip in the menu", Toast.LENGTH_LONG).show();
            startActivity(new Intent(shot2.this, Welcome.class));
            finish();
        } else {
            String clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(this.getApplicationContext(), "tcp://" + ip + ":1883",
                    clientId);
            try {
                IMqttToken token = client.connect();
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // We are connected
                        Log.d(MainActivity.TAG, "onSuccess");
                        Toast.makeText(getApplicationContext(), "success", Toast.LENGTH_LONG).show();

                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        // Something went wrong e.g. connection timeout or firewall problems
                        Toast.makeText(getApplicationContext(), "failed", Toast.LENGTH_LONG).show();
                        Log.e(MainActivity.TAG, "onFailure");
                        Log.e(MainActivity.TAG, exception.getMessage());


                    }
                });
            } catch (MqttException | NullPointerException e) {
                Log.e(MainActivity.TAG, "mqtt" + e.getMessage());
            }

        }
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()){
            case Sensor.TYPE_LINEAR_ACCELERATION:
                y = sensorEvent.values[1];
                Log.w(Welcome.TAG, " " + y);

                if (y < SHAKE_THRESHOLD) {

                    Log.w(MainActivity.TAG, "shaked");
                    Log.i(MainActivity.TAG, "speed= " + y);
                    if(payload == null)
                        Toast.makeText(getApplicationContext(),"an error occured please try again",Toast.LENGTH_LONG).show();
                    else
                        send(payload);
                    send( topic + "^hide^");
                    startActivity(new Intent(shot2.this, Welcome.class));

                    finish();
                }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void send(String payload) {
        Log.w(Welcome.TAG,payload);
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            if (topic != null)
                client.publish("Sling", message);
            else
                Toast.makeText(getApplicationContext(), "please add topic first", Toast.LENGTH_LONG).show();
            // Log.i(MainActivity.TAG,"Failed to publish");
        } catch (UnsupportedEncodingException | MqttException | NullPointerException | IllegalArgumentException e) {
            Log.e(MainActivity.TAG, "error while sending " + e.getMessage());
            //Toast.makeText(getApplicationContext(),"some error occurred please try again",Toast.LENGTH_LONG).show();
            // Log.i(MainActivity.TAG,"Failed to publish");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }
}

