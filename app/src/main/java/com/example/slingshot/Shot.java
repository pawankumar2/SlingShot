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
    private int fallbackk =0;
    // Magnetic rotational data
    private float magnetic[]; //for magnetic rotational data
    private float accels[] = new float[3];
    private float mags[] = new float[3];
    private float[] values = new float[3];
    // azimuth, pitch and roll
    private float y,last_y=0;
    private float azimuth;
    private float pitch;
    static final float ALPHA = 0.25f;
    private static final int SHAKE_THRESHOLD = 250;
    private long lastUpdate = System.currentTimeMillis();
    private String name;
    private String pledge;
    private String topic;
    private String tag;
    int i = 0;
    int j[] = new int[10],k[] = new int[10];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shot);
        SharedPreferences sp = getApplicationContext().getSharedPreferences("data",MODE_PRIVATE);
        name = sp.getString("name","Unknown");
        pledge = sp.getString("image","Unknown");
        String path[] = pledge.split("/");
        pledge = path[path.length -1];
        Log.i(Welcome.TAG,pledge);
        topic = sp.getString("topic",null);
        tag = sp.getString("band_uid",null);
        String ip = sp.getString("mip",null);
        Log.i(MainActivity.TAG,"name: " + name + "\npledge: " + pledge);
        if(ip == null){
            Toast.makeText(getApplicationContext(),"set mqtt ip in the menu",Toast.LENGTH_LONG).show();
            startActivity(new Intent(Shot.this,Welcome.class));
            finish();
        }else{
            String clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(this.getApplicationContext(), "tcp://"+ip+":1883",
                    clientId);
            try {
                IMqttToken token = client.connect();
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // We are connected
                        Log.d(MainActivity.TAG, "onSuccess");
                        Toast.makeText(getApplicationContext(),"success",Toast.LENGTH_LONG).show();

                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        // Something went wrong e.g. connection timeout or firewall problems
                        Toast.makeText(getApplicationContext(),"failed",Toast.LENGTH_LONG).show();
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
        fallbackk++;
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mags = lowPass(event.values.clone(), mags);

            break;
            case Sensor.TYPE_ACCELEROMETER:
                accels = lowPass(event.values.clone(),accels);

                long curTime = System.currentTimeMillis();
                // only allow one update every 100ms.
                //if ((curTime - lastUpdate) > 100) {
                    long diffTime = (curTime - lastUpdate);
                    lastUpdate = curTime;

                    y = accels[1];
              //  Log.i(MainActivity.TAG,"difftime= " + diffTime);

                    float speed = Math.abs(y  - last_y) / diffTime * 10000;
                    //Log.i(MainActivity.TAG," " + speed);


                    if (speed > SHAKE_THRESHOLD && speed < 5000 && mags != null && accels != null && fallbackk >50 ) {
                        mSensorManager.unregisterListener(this);
                        Log.i(MainActivity.TAG,""+fallbackk);
                        Log.w(MainActivity.TAG,"shaked");
                        Log.i(MainActivity.TAG,"speed= " + speed);
                        calc(1);
                        startActivity(new Intent(Shot.this,Welcome.class));
                        last_y = 0;
                        calc(2);
                        disconnect();
                        finish();
                    }
                    last_y = y;
                //}

                break;
        }

        if (mags != null && accels != null) {
            if(i > 3 ){
                calc(0);
                i =0;
            }
            i++;
           // Log.i(MainActivity.TAG," "+ i);

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

    @Override
    public void onBackPressed() {
        calc(2);
        super.onBackPressed();
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
        swap();
        String payload = null ;
        if(n == 0)
             payload = topic + "^moving^"+ (int)  azimuth + "^" + (int)pitch;
        else if (n == 1){
            payload = topic + "^moving^"+ j[9] + "^" + k[9] + "^" + name + "^"+ pledge + "^" + tag;
            Log.i(MainActivity.TAG,payload);
        }
        else if(n == 2)
            payload = topic + "^hide^";
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            if(topic!=null)
                client.publish("Sling", message);
            else
                Toast.makeText(getApplicationContext(),"please add topic first",Toast.LENGTH_LONG).show();
           // Log.i(MainActivity.TAG,"Failed to publish");
        } catch (UnsupportedEncodingException | MqttException | NullPointerException | IllegalArgumentException e) {
            Log.e(MainActivity.TAG,e.getMessage());
           // Log.i(MainActivity.TAG,"Failed to publish");
        }

    }
    public void disconnect(){
        try {
            IMqttToken disconToken = client.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // we are now successfully disconnected
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // something went wrong, but probably we are disconnected anyway
                }
            });
        } catch (MqttException  | NullPointerException e) {
            Log.e(MainActivity.TAG,e.getMessage());
        }
    }
    private void swap(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                j[0] = (int)azimuth;
                k[0] = (int) pitch;
                for(int i = 0;i<j.length-1;i++){
                    int temp = j[i];
                    j[i] = j[i+1];
                    j[i+1] = temp;
                     temp = k[i];
                    k[i] = k[i+1];
                    k[i+1] = temp;
                }
                //Log.i(MainActivity.TAG,"j = " + j + "\nk =  " + k);
            }
        }).start();
    }
}


