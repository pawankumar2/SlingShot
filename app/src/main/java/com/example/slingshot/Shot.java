package com.example.slingshot;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;


public class Shot extends AppCompatActivity implements SensorEventListener {
    private MqttAndroidClient client;
    private SensorManager mSensorManager;
    private float gravity[];
    // Magnetic rotational data
    private float magnetic[]; //for magnetic rotational data
    private float accels[] = new float[3];
    private float mags[] = new float[3];
    private float[] values = new float[3];

    // azimuth, pitch and roll
    private float x,y,z,last_x,last_y,last_z;
    private float azimuth;
    private float pitch;
    private float roll;
    static final float ALPHA = 0.25f;
    private static final int SHAKE_THRESHOLD = 800;
    private long lastUpdate = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shot);
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://172.16.0.11:1883",
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
            Log.e(MainActivity.TAG,e.getMessage());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mags = lowPass(event.values.clone(),mags);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accels = lowPass(event.values.clone(),accels);

                long curTime = System.currentTimeMillis();
                // only allow one update every 100ms.
                if ((curTime - lastUpdate) > 100) {
                    long diffTime = (curTime - lastUpdate);
                    lastUpdate = curTime;

                    x = accels[0];
                    y = accels[1];
                    z = accels[2];

                    float speed = Math.abs(x+y+z - last_x - last_y - last_z) / diffTime * 10000;

                    if (speed > SHAKE_THRESHOLD) {
                        Log.d("sensor", "shake detected w/ speed: " + speed);
                        String payloadq = ( "shake detected w/ speed: " + speed);
                        String topicc = "Sling";
                        byte[] encodedPayload1 = new byte[0];
                        try {
                            encodedPayload1 = payloadq.getBytes("UTF-8");
                            MqttMessage message = new MqttMessage(encodedPayload1);
                            client.publish(topicc, message);
                            Log.i(MainActivity.TAG,"Message sent");
                        } catch (UnsupportedEncodingException | MqttException | NullPointerException e) {
                            Log.e(MainActivity.TAG,e.getMessage());
                        }
                    }
                    last_x = x;
                    last_y = y;
                    last_z = z;
                }

                break;
        }

        if (mags != null && accels != null) {
            gravity = new float[9];
            magnetic = new float[9];
            SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
            float[] outGravity = new float[9];
            SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
            SensorManager.getOrientation(outGravity, values);

            azimuth = ((float) ((values[0] *180)/Math.PI));
            pitch = (float)((values[1]*180/Math.PI));
            roll = (float)((values[2]*180/Math.PI));
            String payload = "\nazimuth: " + (int)  azimuth + "\npitch: " + (int)pitch + "\nroll: " + (int)roll;
            String topic = "Sling";
            byte[] encodedPayload = new byte[0];
            try {
                encodedPayload = payload.getBytes("UTF-8");
                MqttMessage message = new MqttMessage(encodedPayload);
                client.publish(topic, message);
                Log.i(MainActivity.TAG,"Message sent");
            } catch (UnsupportedEncodingException | MqttException | NullPointerException e) {
                Log.e(MainActivity.TAG,e.getMessage());
            }

        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener( this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL,SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);

    }
    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }
}
