package com.example.slingshot;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaScannerConnection;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;

public class Preview extends AppCompatActivity {

    private String path;
    private ImageView preview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        path = getIntent().getStringExtra("path");
        preview = (ImageView) findViewById(R.id.preview);
        addImage();
    }
    private void addImage() {
        Log.i(MainActivity.TAG, "This is where the image came from -> " + path);
        final File imgFile = new File(path);
        if (imgFile.exists()) {
            Bitmap myBitmap = //rotate.rotateBitmap(imgFile.getAbsolutePath()
                    //,
                    BitmapFactory.decodeFile(imgFile.getAbsolutePath());//) ;
            Bitmap frame = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.frame);
            Bitmap finalImage = combineImages(frame, myBitmap);
            preview.setImageBitmap(finalImage);
            Log.i(MainActivity.TAG, "Bitmap added");

            //delImage(imgFile);
            MediaScannerConnection.scanFile(getApplicationContext(), new String[]{imgFile.getPath()}, new String[]{"image/jpeg"}, null);

        }
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
}
