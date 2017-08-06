package com.example.slingshot;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class Camera extends AppCompatActivity implements SurfaceHolder.Callback {

    private FloatingActionButton capture;
    private SurfaceView cameraView;
    private SurfaceHolder surfaceHolder;
    private android.hardware.Camera.PictureCallback jpegCallback;
    private android.hardware.Camera camera;
    private android.hardware.Camera.Parameters params;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        cameraView = (SurfaceView) findViewById(R.id.cameraView);
        capture = (FloatingActionButton) findViewById(R.id.capture);
        surfaceHolder = cameraView.getHolder();
        surfaceHolder.addCallback(this);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.takePicture(null,null,jpegCallback);
            }
        });
        jpegCallback = new android.hardware.Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, android.hardware.Camera camera) {
                shutterSound();
                String path = saveImage(bytes);
                Intent i = new Intent(Camera.this,Preview.class);
                i.putExtra("path",path);
                startActivity(i);
                finish();
            }
        };

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        camera = android.hardware.Camera.open(1);
        params = camera.getParameters();
        params.setPictureSize(getMaxSize().width,getMaxSize().height);
        params.setFocusMode(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        params.setSceneMode(android.hardware.Camera.Parameters.SCENE_MODE_AUTO);
        params.setWhiteBalance(android.hardware.Camera.Parameters.WHITE_BALANCE_AUTO);
        params.setExposureCompensation(0);
        params.setPictureFormat(ImageFormat.JPEG);
        params.setJpegQuality(100);
        params.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_AUTO);
        params.setRotation(270);
        camera.setDisplayOrientation(90);
        camera.setParameters(params);
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            Log.e(MainActivity.TAG, e.getMessage());
        }


    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if(camera !=null ){
            camera.release();
        }
    }
    public void shutterSound(){
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        switch( audio.getRingerMode() ){
            case AudioManager.RINGER_MODE_NORMAL:
                MediaActionSound sound = new MediaActionSound();
                sound.play(MediaActionSound.SHUTTER_CLICK);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                break;
        }
    }
    private static File getOutputMediaFile() {

        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SlingShot");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(MainActivity.TAG, "failed to create directory");
                return null;
            }
        }
        // Create a media file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
//                .format(new Date());
        Long timeStamp = System.currentTimeMillis();
        Log.i(MainActivity.TAG, String.valueOf(timeStamp));
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");
        Log.i(MainActivity.TAG,"Got file");
        Log.i(MainActivity.TAG,mediaFile.getParent() + " - " + mediaFile.getName() + " - " + mediaFile.getPath());

        return mediaFile;
    }
    private String saveImage(byte[] byteArray){
        File pictureFile = getOutputMediaFile();
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(byteArray);
            Log.i(MainActivity.TAG,"file saved");
            fos.close();
            Log.i(MainActivity.TAG,"stream closed");
        } catch (FileNotFoundException e) {
            Log.e(MainActivity.TAG,e.getMessage());

        } catch (IOException e) {
            Log.e(MainActivity.TAG,e.getMessage());
        }
        MediaScannerConnection.scanFile(this, new String[] { pictureFile.getPath() }, new String[] { "image/jpeg" }, null);
        return pictureFile.getPath();
    }
    public android.hardware.Camera.Size getMaxSize(){
        List<android.hardware.Camera.Size> sizes = params.getSupportedPictureSizes();
        android.hardware.Camera.Size size = sizes.get(0);
        for (int i = 0; i < sizes.size(); i++) {
            if (sizes.get(i).width > size.width)
                size = sizes.get(i);
        }
        return size;
    }

}
