package com.example.slingshot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.hardware.Camera;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.melnykov.fab.FloatingActionButton;


public class CameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    Camera camera;

    SurfaceView surfaceView;
    FloatingActionButton capture;
    SurfaceHolder surfaceHolder;
    private android.hardware.Camera.PictureCallback JPEGcallBack;
    Camera.Parameters params;
    Boolean isfront = false;

    private int [] _orientationHistory = new int[5];
    private int _orientationIndex;
    private int _highestIndex = -1;
    private int _currentOrientation;

    private ScaleGestureDetector mScaleDetector;
    float mDist = 0;
    private OrientationEventListener orientationEventListener;

    private SharedPreferences rfid;

    private int [] potrait = {R.drawable.overlay,R.drawable.photoframe};
    private int [] land1 = {R.drawable.overlay3,R.drawable.photoframe3};;
    private int [] land2 = {R.drawable.overlay1,R.drawable.photoframe1};;
    private int frameIndex = 0;
    private  View frameView;
    private View progressForm;
    private ProgressBar progress;
    private View previewForm;
    private ImageView changeCamera;
    private ImageView flash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        //getActionBar().hide();
        capture = (FloatingActionButton) findViewById(R.id.capture);
        surfaceView = (SurfaceView) findViewById(R.id.cameraView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        flash = (ImageView) findViewById(R.id.flash);
        rfid = getApplicationContext().getSharedPreferences("rfid",Context.MODE_PRIVATE);
        frameView = findViewById(R.id.frameView);
        progressForm = findViewById(R.id.progressForm);
        progress = (ProgressBar) findViewById(R.id.previewProgress);
        previewForm = findViewById(R.id.frameView);
        final int cameraCount = Camera.getNumberOfCameras();

        capture.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.takePicture(null,null,JPEGcallBack);
            }
        });
        JPEGcallBack = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] bytes, final Camera camera) {
                shutterSound();
                final int o = getOrientation();
                showProgress(true);
                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... voids) {
                        int angle = 0;
                        if(isfront){
                            if(o == 0)
                                angle = 270;
                            else if(o == 1)
                                angle = 180;
                        }
                        else {
                            if(o == 0)
                                angle = 90;
                            else if(o == 1)
                                angle = 180;
                        }
                        Matrix matrix = new Matrix();
                        matrix.postRotate(angle);
                        Bitmap image = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                        Bitmap rotatedBitmap = Bitmap.createBitmap(image , 0, 0, image.getWidth(), image.getHeight(), matrix, true);
                        ByteArrayOutputStream boas = new ByteArrayOutputStream();
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG,100,boas);
                        String path = saveImage(boas.toByteArray());
                        Log.i(MainActivity.TAG,path);

                        return path;
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        super.onPostExecute(s);
                        Intent i = new Intent(CameraActivity.this,Preview.class);
                        i.putExtra("path",s);
                        i.putExtra("orientation",o);
                        if(frameIndex == potrait.length)
                            i.putExtra("frame",--frameIndex);
                        else if (frameIndex == -1)
                            i.putExtra("frame",0);
                        else
                            i.putExtra("frame",frameIndex);
                        startActivity(i);
                        finish();
                        showProgress(false);

                        if(camera != null) {
                            camera.stopPreview();
                            camera.release();
                        }
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
        if(potrait.length != 0 && land1.length != 0
                && land2.length != 0 && land1.length == potrait.length
                && potrait.length == land2.length){

            orientationEventListener.enable();
            frameView.setOnTouchListener(new OnSwipe(getApplicationContext()){

                public void onSwipeRight() {
                    int i = getOrientation();
                    frameIndex--;
                    if(i == 0){
                        if(frameIndex >=0 && frameIndex < potrait.length){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                setFrame(potrait[frameIndex]);
                            }
                            Log.i(MainActivity.TAG,"frame changed -> right");}
                        else
                            frameIndex++;
                    }
                    else if(i == 1){
                        if(frameIndex >=0 && frameIndex < land2.length){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                setFrame(land2[frameIndex]);
                            }
                            Log.i(MainActivity.TAG,"frame changed -> right");}
                        else
                            frameIndex++;
                    }
                    else if(i == 3){
                        if(frameIndex >=0 && frameIndex < land1.length){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                setFrame(land1[frameIndex]);
                            }
                            Log.i(MainActivity.TAG,"frame changed -> right");}
                        else
                            frameIndex++;
                    }


                }
                public void onSwipeLeft() {
                    int i = getOrientation();
                    frameIndex++;
                    if(i == 0){
                        if(frameIndex >=0 && frameIndex < potrait.length){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                setFrame(potrait[frameIndex]);
                            }
                            Log.i(MainActivity.TAG,"frame changed -> left");}
                        else
                            frameIndex--;
                    }
                    else if(i == 1){
                        if(frameIndex >=0 && frameIndex < land2.length){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                setFrame(land2[frameIndex]);
                            }
                            Log.i(MainActivity.TAG,"frame changed -> left");}
                        else
                            frameIndex--;
                    }
                    else if(i == 3){
                        if(frameIndex >=0 && frameIndex < land1.length){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                setFrame(land1[frameIndex]);
                            }
                            Log.i(MainActivity.TAG,"frame changed -> left");}
                        else
                            frameIndex--;
                    }
                }

            });

        }
        else
            orientationEventListener.disable();
        changeCamera = (ImageView) findViewById(R.id.changeCamera);
        if(cameraCount == 1){
            changeCamera.setVisibility(View.GONE);
            changeCamera.setEnabled(false);
        }

        changeCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isfront){
                    changeCamera.setImageResource(R.mipmap.ic_flip_to_back_white_36dp);
                    if(camera != null){
                        camera.release();
                    }
                    isfront = true;
                    setCamera(true);
                }
                else{
                    if(camera != null){
                        camera.release();
                    }
                    changeCamera.setImageResource(R.mipmap.ic_flip_to_front_white_36dp);
                    isfront = false;
                    setCamera(false);
                }



            }
        });
        flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Bitmap imageFromLayout = ((BitmapDrawable)flash.getDrawable())
                        .getBitmap();
                Bitmap flashOff = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.ic_flash_off_white_36dp);
                if(imageFromLayout.sameAs(flashOff)){
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    camera.setParameters(params);
                    flash.setImageResource(R.mipmap.ic_flash_on_white_36dp);
                }
                else {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(params);
                    flash.setImageResource(R.mipmap.ic_flash_off_white_36dp);
                }
            }
        });

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

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        isfront = getIntent().getBooleanExtra("isfront",false);
        if(isfront)
            changeCamera.setImageResource(R.mipmap.ic_flip_to_back_white_36dp);
        setCamera(isfront);

    }

    private void setCamera(final boolean isFront) {
        int cameraIndex = 0;
        if(isFront){
            cameraIndex = openFrontFacingCamera();
            Log.i(MainActivity.TAG,"Front facing");
        }
        try{
            camera = Camera.open(cameraIndex);
        }catch(Exception e){
            Log.e(MainActivity.TAG,e.getMessage());
        }
        params = camera.getParameters();


        if(checkSize(getWidth(),getHeight()))
            params.setPictureSize(getWidth(), getHeight());
        else {
            Camera.Size size = getMaxSize();
            params.setPictureSize(size.width,size.height);
        }
        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        else{
            flash.setEnabled(false);
            flash.setVisibility(View.INVISIBLE);
        }
        if(!isFront && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            params.setExposureCompensation(0);
        }
        else{
            camera.setDisplayOrientation(180);
        }

        params.setPictureFormat(ImageFormat.JPEG);
        params.setJpegQuality(100);
        camera.setDisplayOrientation(90);
        camera.setParameters(params);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            camera.enableShutterSound(true);
        }
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            Log.e(MainActivity.TAG, e.getMessage());
        }

    }

    private boolean checkSize(int width, int height) {
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        for (Camera.Size size : sizes) {
            if(size.width == width && size.height == height){
                return true;
            }
        }
        return false;
    }


    private int openFrontFacingCamera()
    {
        int cameraCount = Camera.getNumberOfCameras();
        if (cameraCount == 2)
            return  1;
        return 0;

    }
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if(camera !=null ){
            camera.release();
        }
        if (orientationEventListener!= null) {
            orientationEventListener.disable();
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(CameraActivity.this,MainActivity.class));
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        orientationEventListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        orientationEventListener.disable();
    }

    public int getHeight(){return getMaxSize().height;}
    public int getWidth(){return getMaxSize().width;}
    public Camera.Size getMaxSize(){
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        Camera.Size size = sizes.get(0);
        for (int i = 0; i < sizes.size(); i++) {
            if (sizes.get(i).width > size.width)
                size = sizes.get(i);
        }
        return size;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Get the pointer ID
        Camera.Parameters params = camera.getParameters();
        int action = event.getAction();

        if (event.getPointerCount() > 1) {
            // handle multi-touch events
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                mDist = getFingerSpacing(event);
            } else if (action == MotionEvent.ACTION_MOVE
                    && params.isZoomSupported()) {
                camera.cancelAutoFocus();
                handleZoom(event, params);
            }
        } else {
            // handle single touch events
            if (action == MotionEvent.ACTION_UP) {
                if(!isfront)
                    handleFocus(event, params);
            }
        }
        return true;
    }

    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            // zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < mDist) {
            // zoom out
            if (zoom > 0)
                zoom--;
        }
        mDist = newDist;
        params.setZoom(zoom);
        camera.setParameters(params);
    }

    public void handleFocus(MotionEvent event, Camera.Parameters params) {
        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null
                && supportedFocusModes
                .contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    // currently set to auto-focus on single touch
                }
            });
        }
    }

    /** Determine the space between the first two fingers */
    private float getFingerSpacing(MotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }
    protected int getOrientation(){
        if (_highestIndex < 0) return 0;

        Arrays.sort(_orientationHistory);
        return _orientationHistory[_highestIndex / 2];
    }

    protected void orientationChanged(int lastOrientation, int currentOrientation) {

        if(currentOrientation == 1){
            int frameTobeSet;
            if(frameIndex == land2.length)
                frameTobeSet = land2[--frameIndex];
            else if (frameIndex == -1)
                frameTobeSet = land2[++frameIndex];
            else
                frameTobeSet = land2[frameIndex];
            setFrame(frameTobeSet);

            //params.setRotation(180);
        }
        else if(currentOrientation == 3){
            int frameTobeSet;
            if(frameIndex == land1.length)
                frameTobeSet = land1[--frameIndex];
            else if (frameIndex == -1)
                frameTobeSet = land1[++frameIndex];
            else
                frameTobeSet = land1[frameIndex];
            setFrame(frameTobeSet);
            // params.setRotation(0);
        }
        else if(getOrientation() == 0){
            int frameTobeSet;
            if(frameIndex == potrait.length)
                frameTobeSet = potrait[--frameIndex];
            else if (frameIndex == -1)
                frameTobeSet = potrait[++frameIndex];
            else
                frameTobeSet = potrait[frameIndex];
            setFrame(frameTobeSet);
        }
        Log.d(MainActivity.TAG, "Orientation changed to " + currentOrientation + " from " + lastOrientation);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        orientationEventListener.disable();
    }
    private void setFrame(int frame){
        surfaceView.setForeground(getDrawable(frame));
    }


}

