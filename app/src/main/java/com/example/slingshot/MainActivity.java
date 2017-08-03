package com.example.slingshot;

import android.content.Context;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private SensorManager mSensorManager;

    public static final String TAG = "MainActivity";
    private Sensor magnetic;
    private Sensor accelero;
    private SensorManager sensorManager;
    private TextView magneticText;
    private TextView acceleroText;

    SensorEventListener mListner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        String clientId = MqttClient.generateClientId();
        final MqttAndroidClient client =
                new MqttAndroidClient(this.getApplicationContext(), "tcp://192.168.1.20:1883",
                        clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "onSuccess");
                    String topic = "social";
                    String payload = "hello world";
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = payload.getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                        Log.i(TAG,"Message sent");
                    } catch (UnsupportedEncodingException | MqttException e) {
                        Log.e(TAG,e.getMessage());
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.e(TAG, "onFailure");
                    Log.e(TAG, exception.getMessage());


                }
            });
        } catch (MqttException e) {
            Log.e(TAG,e.getMessage());
        }

        sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelero = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticText = (TextView) findViewById(R.id.magnetic);
        acceleroText = (TextView) findViewById(R.id.accelero);

//        String clientId = MqttClient.generateClientId();
//        final MqttAndroidClient client =
//                new MqttAndroidClient(this.getApplicationContext(), "tcp://192.168.1.20:1883",
//                        clientId);
//
//        try {
//            IMqttToken token = client.connect();
//            token.setActionCallback(new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    // We are connected
//                    Log.d(TAG, "onSuccess");
//                    String topic = "social";
//                    String payload = "hello world";
//                    byte[] encodedPayload = new byte[0];
//                    try {
//                        encodedPayload = payload.getBytes("UTF-8");
//                        MqttMessage message = new MqttMessage(encodedPayload);
//                        client.publish(topic, message);
//                        Log.i(TAG,"Message sent");
//                    } catch (UnsupportedEncodingException | MqttException e) {
//                        Log.e(TAG,e.getMessage());
//                    }
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    // Something went wrong e.g. connection timeout or firewall problems
//                    Log.e(TAG, "onFailure");
//                    Log.e(TAG, exception.getMessage());
//
//
//                }
//            });
//        } catch (MqttException e) {
//            Log.e(TAG,e.getMessage());
//        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            Log.i(TAG,"Magnetic");
            float[] values = sensorEvent.values;
            magneticText.setText("Magnetic:\n" + "x: " + values[0] + "\n" + "y: " + values[1] + "\n" +  "z: " + values[2] );

        }
        else if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            Log.i(TAG,"Accelerometer");
            float[] values = sensorEvent.values;
            acceleroText.setText("Accelerometer:\n" + "x: " + values[0] + "\n" + "y: " + values[1] + "\n" +  "z: " + values[2] );

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener( this,magnetic,SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener( this,accelero,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this,magnetic);

        sensorManager.unregisterListener(this,accelero);
    }

}
