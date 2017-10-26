package com.example.slingshot;


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
    private static final int SHAKE_THRESHOLD = 200;
    private long lastUpdate = System.currentTimeMillis();
    private String name;
    private String pledge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shot);
        SharedPreferences sp = getApplicationContext().getSharedPreferences("data",MODE_PRIVATE);
        name = sp.getString("name","Unknown");
        pledge = sp.getString("pledge","Unknown");
        String ip = sp.getString("mip",null);
        if(ip == null){
            Toast.makeText(getApplicationContext(),"set mqtt ip in the menu",Toast.LENGTH_LONG).show();
            startActivity(new Intent(Shot.this,Welcome.class));
            finish();
        }else{
            String clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(this.getApplicationContext(), "tcp://"+ ip +":1883",
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
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
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
                //if ((curTime - lastUpdate) > 100) {
                    long diffTime = (curTime - lastUpdate);
                    lastUpdate = curTime;

                    x = accels[0];
                    y = accels[1];
                    z = accels[2];

                    float speed = Math.abs(x+y+z - last_x - last_y - last_z) / diffTime * 10000;


                    if (speed > SHAKE_THRESHOLD && mags != null && accels != null) {
                        Log.w(MainActivity.TAG,"shaked");

                        calc(1);
                        startActivity(new Intent(Shot.this,Welcome.class));
                        finish();
                    }
                    last_x = x;
                    last_y = y;
                    last_z = z;
                //}

                break;
        }

        if (mags != null && accels != null) {
            calc(0);
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
    public void calc(int n){
        gravity = new float[9];
        magnetic = new float[9];
        SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
        float[] outGravity = new float[9];
        SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
        SensorManager.getOrientation(outGravity, values);
        azimuth = ((float) ((values[0] *180)/Math.PI)+180);
        pitch = (float)((values[1]*180/Math.PI)+90);
        String payload = "don't know";
        if(n == 0)
             payload = "moving^"+ (int)  azimuth + "^" + (int)pitch;
        else if (n == 1)
            payload = "moving^"+ (int)  azimuth + "^" + (int)pitch + "^" + name + "^"+ pledge;
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
}


