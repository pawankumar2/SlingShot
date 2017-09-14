package com.example.slingshot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public class Shot extends AppCompatActivity implements SensorEventListener {
    private Sensor rotation;
    private SensorManager sensorManager;
    private float x[];
    private float y[];
    private float z[];
    private int i = 0;
    private MqttAndroidClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shot);
        sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        x = new float[10];
        y = new float[10];
        z = new float[10];
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://192.168.1.39:1883",
                clientId);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            float[] values = sensorEvent.values;
            x[i] = values[0];
            y[i] = values[1];
            z[i] = values[2];
            i++;
            Log.i(MainActivity.TAG,"Rotation:\n"
                    + "x: " + values[0] + "\n"
                    + "y: " + values[1] + "\n"
                    +  "z: " + values[2]);
            if(i == 10){
                final String data = Math.toDegrees(Math.atan2(y[9],x[9]))
                        + "\nfie: "+ Math.toDegrees(Math.atan2(y[9],z[9]));
                i = 0;
                try {
                    IMqttToken token = client.connect();
                    token.setActionCallback(new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            // We are connected
                            Log.d(MainActivity.TAG, "onSuccess");
                            String topic = "Sling";
                            String payload = data;
                            byte[] encodedPayload = new byte[0];
                            try {
                                encodedPayload = payload.getBytes("UTF-8");
                                MqttMessage message = new MqttMessage(encodedPayload);
                                client.publish(topic, message);
                                Log.i(MainActivity.TAG,"Message sent");
                            } catch (UnsupportedEncodingException | MqttException e) {
                                Log.e(MainActivity.TAG,e.getMessage());
                            }
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            // Something went wrong e.g. connection timeout or firewall problems
                            Log.e(MainActivity.TAG, "onFailure");
                            Log.e(MainActivity.TAG, exception.getMessage());


                        }
                    });
                } catch (MqttException e) {
                    Log.e(MainActivity.TAG,e.getMessage());
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener( this,rotation, SensorManager.SENSOR_DELAY_NORMAL);
        // sensorManager.registerListener( this,gyro,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //sensorManager.unregisterListener(this,gyro);

        sensorManager.unregisterListener(this,rotation);
    }
}
