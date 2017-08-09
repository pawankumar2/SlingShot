package com.example.slingshot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;

import com.melnykov.fab.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Camera extends AppCompatActivity implements SurfaceHolder.Callback {

    private FloatingActionButton capture;
    private SurfaceView cameraView;
    private SurfaceHolder surfaceHolder;
    private android.hardware.Camera.PictureCallback jpegCallback;
    private android.hardware.Camera camera;
    private android.hardware.Camera.Parameters params;
    private int [] _orientationHistory = new int[5];
    private int _orientationIndex;
    private int _highestIndex = -1;
    private int _currentOrientation;
    private OrientationEventListener orientationEventListener;
    private View progressForm;
    private ProgressBar progress;
    private View previewForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        cameraView = (SurfaceView) findViewById(R.id.cameraView);
        capture = (FloatingActionButton) findViewById(R.id.capture);
        progress = (ProgressBar) findViewById(R.id.previewProgress);
        progressForm = findViewById(R.id.progressForm);
        previewForm = findViewById(R.id.frameView);
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
            public void onPictureTaken(final byte[] bytes, android.hardware.Camera camera) {
                final int i = getOrientation();
                shutterSound();
                showProgress(true);
                new AsyncTask<Object, Object, String>() {

                    @Override
                    protected String doInBackground(Object... strings) {

                            int angle = 0;
                            Bitmap frame =  BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.overlay);
                            Bitmap image = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                            Matrix matrix = new Matrix();
                            if(i == 0)
                                angle = 270;
                            else if(i == 1)
                                angle = 180;

                            matrix.postRotate(angle);
                            Bitmap rotatedBitmap = Bitmap.createBitmap(image , 0, 0, image.getWidth(), image.getHeight(), matrix, true);

                            Bitmap combinedImage = combineImages(frame,rotatedBitmap);
                            ByteArrayOutputStream boas = new ByteArrayOutputStream();
                            combinedImage.compress(Bitmap.CompressFormat.JPEG,100,boas);
                            String path = saveImage(boas.toByteArray());
                            return path;
                        }

                    @Override
                    protected void onPostExecute(String aVoid) {
                        super.onPostExecute(aVoid);
                        showProgress(false);
                        Intent intent = new Intent(Camera.this,Preview.class);
                        intent.putExtra("path",aVoid);
                        startActivity(intent);
                    }
                }.execute();

            }
        };
        orientationEventListener = new OrientationEventListener(getApplicationContext()) {

            @Override
            public void onOrientationChanged(int i) {
                i = i + 45;
                if (i > 360) i = i - 360;
                int orientation = i / 90;

                //I use a history in order to smooth out noise
                //and don't start sending events about the change until this history is filled
                if (_orientationIndex > _highestIndex) {
                    _highestIndex = _orientationIndex;
                }
                _orientationHistory[_orientationIndex] = orientation;
                _orientationIndex ++;

                if (_orientationIndex == _orientationHistory.length) {
                    _orientationIndex = 0;
                }

                int lastOrientation = _currentOrientation;
                //compute the orientation using above method
                _currentOrientation = getOrientation();

                if (_highestIndex == _orientationHistory.length - 1 && lastOrientation != _currentOrientation) {
                    //enough data to say things changed
                    orientationChanged(lastOrientation, _currentOrientation);

                }
            }
        };
        orientationEventListener.enable();

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
        //params.setRotation(270);
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
    protected int getOrientation(){
        if (_highestIndex < 0) return 0;

        Arrays.sort(_orientationHistory);
        return _orientationHistory[_highestIndex / 2];
    }

    protected void orientationChanged(int lastOrientation, int currentOrientation) {

        if(currentOrientation == 1){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraView.setForeground(getDrawable(R.drawable.overlay1));
            }
           // params.setRotation(180);
        }
        else if(currentOrientation == 3){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraView.setForeground(getDrawable(R.drawable.overlay3));
            }
            //params.setRotation(0);
        }
        else if(getOrientation() == 0){
            //params.setRotation(270);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraView.setForeground(getDrawable(R.drawable.overlay));
            }
        }
        camera.setParameters(params);
        Log.d(MainActivity.TAG, "Orientation changed to " + currentOrientation + " from " + lastOrientation);
    }
    public Bitmap combineImages(Bitmap frame, Bitmap image) {

        Bitmap cs = null;
        Bitmap rs = null;

        rs = Bitmap.createScaledBitmap(frame, image.getWidth(),
                image.getHeight(), true);

        Bitmap smallImage = Bitmap.createScaledBitmap(image,image.getWidth(),image.getHeight(),true);
        cs = Bitmap.createBitmap(rs.getWidth(), rs.getHeight(),
                Bitmap.Config.RGB_565);

        Canvas comboImage = new Canvas(cs);

        comboImage.drawBitmap(smallImage, 0, 0, null);
        comboImage.drawBitmap(rs, 0, 0, null);


        if (rs != null) {
            rs.recycle();
            rs = null;
        }
        Runtime.getRuntime().gc();

        return cs;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(orientationEventListener != null)
            orientationEventListener.disable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(orientationEventListener == null)
            orientationEventListener.enable();
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            previewForm.setVisibility(show ? View.GONE : View.VISIBLE);
            previewForm.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    previewForm.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressForm.setVisibility(show ? View.VISIBLE : View.GONE);
            progress.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressForm.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            progressForm.setVisibility(show ? View.VISIBLE : View.GONE);
            previewForm.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
