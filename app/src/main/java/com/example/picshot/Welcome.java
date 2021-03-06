package com.example.picshot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.print.PrintHelper;
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

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
    public static final String TAG = "MainActivity";
    File portrait0;
    private String appDataLocation;
    private File frames;
    private final static int SELECT_PORTRAIT = 3;
    private static final int SELECT_BACKGROUND = 4;
    File background;
    View mProgressView;
    View mWelcomeFormView;
    private Bitmap image = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        new Permissions(getApplicationContext(),Welcome.this).takePhoto();
        Button start = (Button) findViewById(R.id.start);
        sp = getApplicationContext().getSharedPreferences("data", MODE_PRIVATE);

        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        ContextWrapper cWrapper = new ContextWrapper(this);
        appDataLocation = cWrapper.getFilesDir().getAbsolutePath();

        mProgressView = findViewById(R.id.welcome_progress);
        mWelcomeFormView = findViewById(R.id.welcome_form);
        frames = new File(appDataLocation + "/Frames");
        frames.mkdir();
        background = new File(appDataLocation + "/Frames/background");
        background.mkdir();
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("band_uid",null);
        editor.putString("name",null);
        editor.commit();
        portrait0 = new File(appDataLocation + "/Frames/portrait0");
        portrait0.mkdir();
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                set(2);
            }
        });
    }

    public void print(){
        if(image != null){
            PrintHelper photoPrinter = new PrintHelper(Welcome.this);
            photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            photoPrinter.printBitmap("image", image);
        }
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

            send(0);
        }
        else if(item.getItemId() == R.id.topic){
            set(3);
        }
        else if (item.getItemId() == R.id.ip)
            set(0);
//        else if (item.getItemId() == R.id.campaign)
//            set(4);
        else if (item.getItemId() == R.id.devicename)
            set(5);
//        else if(item.getItemId() == R.id.shut){
//            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setMessage("Are you sure you want to shutdown the system?")
//                    .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            send(1);
//                        }
//
//                    }).setNegativeButton("No", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//
//                }
//            });
//            builder.create().show();
//        }
        else if(item.getItemId() == R.id.portrait){
            selectImage(SELECT_PORTRAIT);
        }
        else if(item.getItemId() == R.id.background){
            selectImage(SELECT_BACKGROUND);
        }
            else if(item.getItemId() == R.id.delete) {
            AlertDialog.Builder delete = new AlertDialog.Builder(Welcome.this);
            delete.setMessage("Are you sure you want to delete all the images?")
                    .setTitle("Delete")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int j) {

                            if (portrait0.listFiles().length != 0)
                                portrait0.listFiles()[0].delete();
                            if(background.listFiles().length != 0)
                                background.listFiles()[0].delete();

                            Toast.makeText(getApplicationContext(), portrait0.listFiles().length + " frames remaining", Toast.LENGTH_LONG).show();


                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
        else if(item.getItemId() == R.id.hold){
            set(6);
        }
        else if (item.getItemId()==R.id.lock){
            boolean lock;
            if(!item.isChecked()){
                lock = true;
                item.setChecked(true);
            }
            else{
                lock = false;
                item.setChecked(false);
            }
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("lock",lock);
            editor.commit();
        }

        return true;
    }
    public void setBackground(){
        if(background.listFiles().length != 0) {
            findViewById(R.id.welcome).setBackground(new BitmapDrawable(getResources(), BitmapFactory.decodeFile(background.listFiles()[0].getPath())));
        }
    }
    public void selectImage(int imageType){
        Intent intent = new Intent();
        intent.setType("image/png");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select File"),imageType);
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenu(menu);
        return super.onPrepareOptionsMenu(menu);
    }



    private void updateMenu(Menu menu) {
        MenuItem lock = menu.findItem(R.id.lock);
        Log.d(TAG,"lock is " + sp.getBoolean("lock",false));
        if(sp.getBoolean("lock",false))
            lock.setChecked(true);
        else
            lock.setChecked(false);
    }
    public void send (int i){
        String topic = sp.getString("topic",null);
        String payload = null;
        if(i == 0)
            payload = topic + "^remove^";
        else if (i == 1 )
            payload = topic + "^shutdown^";
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
    public void set(final int n){
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
       final View layout = inflater.inflate(R.layout.namedialog,
                (ViewGroup) findViewById(R.id.nameLayout));
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText text =  layout.findViewById(R.id.nameDialog);
        builder.setView(layout);

        if(n ==0) {
            builder.setTitle("Enter IP");
            text.setHint(sp.getString("mip","IP"));
        }
        else if(n == 2){
            builder.setTitle("Enter your TagID");
            text.setHint("TagID");
        }
        else if(n == 3){
            builder.setTitle("Enter Topic");
            text.setHint(sp.getString("topic","topic"));
        }
        else if(n == 4){
            builder.setTitle("Enter campaign");
            text.setHint(sp.getString("campaign","campaign"));
        }
        else if(n==5){
            builder.setTitle("Enter devicename");
            text.setHint(sp.getString("devicename","devicename"));
        }
        else if (n == 6){
            builder.setTitle("Enter threshold");
            text.setHint(sp.getString("hold","-7"));
        }
        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.i(MainActivity.TAG,"OKAY Pressed");

                String converted = text.getText().toString().replace(" ", "");
                if(converted.length() != 0){
                    Log.i(MainActivity.TAG,"\nname = " + text);
                    SharedPreferences.Editor editor = sp.edit();
                    if(n==0) {
                        editor.putString("mip", converted);
                        connect(converted);
                    }
                    else if (n==2) {
                        editor.putString("band_uid", converted);
                        //getUid(converted);
                        startActivity(new Intent(Welcome.this, CameraActivity.class));
                    }
                    else if(n==3){
                        editor.putString("topic",converted);
                    }
                    else if(n==4)
                        editor.putString("campaign",converted);
                    else if(n==5)
                        editor.putString("devicename",converted);
                    else if (n == 6)
                        editor.putString("hold",converted);
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
        disconnect();
        super.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        new Fullscreen(findViewById(R.id.welcome)).hideSystemUI();
        invalidateOptionsMenu();
        String ip = sp.getString("mip",null);
        connect(ip);
        mSensorManager.registerListener( this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL,SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);

        Boolean print = sp.getBoolean("print",false);
        Log.i(Welcome.TAG,"from welcome activity value of print is "+print);
        String path = sp.getString("image",null);
        if(path != null)
            image = BitmapFactory.decodeFile(path);
        else
            Log.i(TAG,"print path is null");
        if(print){
            print();
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("print",false);
            editor.commit();
        }
        new Uploader2(getApplicationContext(),sp.getString("devicename",null));
        setBackground();
    }
    public void connect(String mip){
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

    private void getUid(final String tagid) {

        String campaign = sp.getString("campaign",null);
        if(campaign == null)
            Toast.makeText(getApplicationContext(),"Enter campaign ",Toast.LENGTH_LONG).show();
        else{
            showProgress(true);
            final String url = "http://socialact.in/api/social-users/"+campaign ;
            RequestQueue queue = Volley.newRequestQueue(this);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    JSONArray arrayResponse = null;
                    String name = "";
                    String uid = "";
                    try {
                        arrayResponse = response.getJSONArray("data");
                        for(int i = 0; i<arrayResponse.length();i++){
                            try {
                                JSONObject user = arrayResponse.getJSONObject(i);

                                if(tagid.equals(user.getString("band_number"))){
                                    uid = user.getString("band_uid");
                                    name = user.getString("name");
                                    Log.i(MainActivity.TAG,"uid = " + user.getString("band_uid") + "\n name =  "+ user.getString("name"));
                                    break;
                                }
                            } catch (JSONException e) {
                                Log.e(MainActivity.TAG,e.getMessage());
                            }

                        }
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("band_uid",uid);
                        editor.putString("name",name);
                        editor.commit();
                        showProgress(false);
                    } catch (JSONException e) {
                        showProgress(false);
                        Log.e(MainActivity.TAG,e.getMessage());
                    }


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i(MainActivity.TAG,error.toString());
                    showProgress(false);
                }
            });
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                    5,
                    2));
            queue.add(jsonObjectRequest);

        }
    }
    public void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_PORTRAIT || requestCode == SELECT_BACKGROUND){
            if (resultCode == RESULT_OK) {
                showProgress(true);
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        onSelectFromGalleryResult(data,requestCode);
                        Log.i(TAG,"got result for image");
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        Toast.makeText(getApplicationContext(),"Frame was added",Toast.LENGTH_SHORT).show();
                        setBackground();
                        showProgress(false);
                    }
                }.execute();

            }
        }

//        if(requestCode == SELECT_LAND) {
//            if (resultCode == RESULT_OK) {
//                showProgress(true);
//                new AsyncTask<Void, Void, Void>() {
//                    @Override
//                    protected Void doInBackground(Void... voids) {
//                        onSelectFromGalleryResult(data, requestCode);
//                        return null;
//                    }
//
//                    @Override
//                    protected void onPostExecute(Void aVoid) {
//                        super.onPostExecute(aVoid);
//                        Toast.makeText(getApplicationContext(), "Frame was added", Toast.LENGTH_SHORT).show();
//                        showProgress(false);
//                    }
//                }.execute();

//                Uri imageUri = CropImage.getPickImageResultUri(this, data);
//                startCropImageActivity(imageUri);
          //  }

//        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
//            CropImage.ActivityResult result = CropImage.getActivityResult(data);
//            if (resultCode == RESULT_OK) {
//                Uri resultUri = result.getUri();
//                Log.i(TAG, resultUri.toString());
//                Bitmap bitmap = null;
//                try {
//                    bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), resultUri);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                // saveFile(resultUri, portrait0);
//                addFrame(bitmap,portrait0);
//            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
//                Exception error = result.getError();
//
//            }
//        }
        }
    private void onSelectFromGalleryResult(Intent data,int requestCode) {
        Log.i(TAG, " " + requestCode);
        Bitmap bm = null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (requestCode == SELECT_PORTRAIT)
            addFrame(bm, portrait0);
        else if (requestCode == SELECT_BACKGROUND)
            addFrame(bm,background);
//        else {
//            Matrix matrix1 = new Matrix();
//            matrix1.postRotate(90);
//            Bitmap rotateLand1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix1, true);
//            Matrix matrix2 = new Matrix();
//            matrix2.postRotate(270);
//            Bitmap rotateLand2 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix2, true);
//            addFrame(rotateLand1, land1);
//            addFrame(rotateLand2, land2);
        //}


    }
    //}



    private void addFrame(Bitmap bitmap, File file1 ){
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
            File file = new File(file1.getAbsolutePath() + "/frame" + file1.listFiles().length + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            Log.w(Welcome.TAG,file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mWelcomeFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mWelcomeFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mWelcomeFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mWelcomeFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

}
