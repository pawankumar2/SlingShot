package com.example.slingshot;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{

    public static final String TAG = "MainActivity";
    private static final String[] PARAMS_TAKE_PHOTO = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int RESULT_PARAMS_TAKE_PHOTO = 11;

    private static final int REQUEST_PERMISSION_SETTING = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        takePhoto();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent (MainActivity.this,Data    .class));
                finish();
            }
        }, 5000);

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
    private void takePhoto() {

        if (canTakePhoto()) {


        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

            Toast.makeText(this, "You should give permission", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, netPermisssion(PARAMS_TAKE_PHOTO), RESULT_PARAMS_TAKE_PHOTO);

        } else {
            ActivityCompat.requestPermissions(this, netPermisssion(PARAMS_TAKE_PHOTO), RESULT_PARAMS_TAKE_PHOTO);
        }

    }

    //  This method return  permission denied String[] so we can request again
    private String[] netPermisssion(String[] wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String permission : wantedPermissions) {
            if (!hasPermission(permission)) {
                result.add(permission);
            }
        }

        return (result.toArray(new String[result.size()]));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == RESULT_PARAMS_TAKE_PHOTO) {

            if (canTakePhoto()) {


            } else if (!(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE))) {


                final AlertDialog.Builder settingDialog = new AlertDialog.Builder(MainActivity.this);
                settingDialog.setTitle("Permissioin");
                settingDialog.setMessage("Now you need to enable permisssion from the setting because without permission this app won't run properly \n\n  goto -> setting -> appInfo");
                settingDialog.setCancelable(false);

                settingDialog.setPositiveButton("Setting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        dialogInterface.cancel();

                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                        Toast.makeText(getBaseContext(), "Go to Permissions to Grant all permission ENABLE", Toast.LENGTH_LONG).show();

                    }
                });
                settingDialog.show();

                Toast.makeText(this, "You need to grant permission from setting", Toast.LENGTH_SHORT).show();

            }

        }

    }
    private boolean canTakePhoto() {
        return (hasPermission(Manifest.permission.CAMERA) && hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }
    private boolean hasPermission(String permissionString) {
        return (ContextCompat.checkSelfPermission(this, permissionString) == PackageManager.PERMISSION_GRANTED);
    }

}
