package com.example.picshot;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by lenovo-05 on 14-11-2017.
 */

public class Permissions extends Activity {
   private Context context ;

    public Permissions(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    private Activity activity;
    private static final String[] PARAMS_TAKE_PHOTO = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int RESULT_PARAMS_TAKE_PHOTO = 11;

    private static final int REQUEST_PERMISSION_SETTING = 99;

    public void takePhoto() {

        if (canTakePhoto()) {


        } else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {

            Toast.makeText(activity, "You should give permission", Toast.LENGTH_SHORT).show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(PARAMS_TAKE_PHOTO,RESULT_PARAMS_TAKE_PHOTO);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(PARAMS_TAKE_PHOTO,RESULT_PARAMS_TAKE_PHOTO);
            }
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


            } else if (!(ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                    || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE))) {


                final AlertDialog.Builder settingDialog = new AlertDialog.Builder(activity);
                settingDialog.setTitle("Permissioin");
                settingDialog.setMessage("Now you need to enable permisssion from the setting because without permission this app won't run properly \n\n  goto -> setting -> appInfo");
                settingDialog.setCancelable(false);

                settingDialog.setPositiveButton("Setting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        dialogInterface.cancel();

                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                        intent.setData(uri);
                        activity.startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                        Toast.makeText(activity, "Go to Permissions to Grant all permission ENABLE", Toast.LENGTH_LONG).show();

                    }
                });
                settingDialog.show();

                Toast.makeText(activity, "You need to grant permission from setting", Toast.LENGTH_SHORT).show();

            }

        }

    }
    private boolean canTakePhoto() {
        return (hasPermission(Manifest.permission.CAMERA) && hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }
    private boolean hasPermission(String permissionString) {
        return (ContextCompat.checkSelfPermission(activity, permissionString) == PackageManager.PERMISSION_GRANTED);
    }

}

