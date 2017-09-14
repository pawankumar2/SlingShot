package com.example.slingshot;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.Handler;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.example.slingshot.R.id.imageView;

public class Preview extends AppCompatActivity {

    private String path;
    private ImageView preview;
    private CheckBox signature;
    private CheckBox name;
    private Button moveForward;
    private LayoutInflater inflater;
    private View layout;
    private AlertDialog.Builder nameBuilder;
    private AlertDialog nameDialog;
    private SharedPreferences pref;
    private Button retake;
    private int orientation;
    private int [] portrait = {R.drawable.overlay,R.drawable.photoframe};
    private int [] land = {R.drawable.overlay0,R.drawable.photoframe0};
    private  Bitmap combinedImage;
    private int applyFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        path = getIntent().getStringExtra("path");
        orientation = getIntent().getIntExtra("orientation",0);
        applyFrame = getIntent().getIntExtra("frame",0);
        preview = (ImageView) findViewById(R.id.preview);
        signature = (CheckBox) findViewById(R.id.signatureCheckBox);
        name = (CheckBox) findViewById(R.id.nameCheckBox);
        moveForward = (Button) findViewById(R.id.moveForward);
        retake = (Button) findViewById(R.id.retake);
        pref = getApplicationContext().getSharedPreferences("data", Context.MODE_PRIVATE);
        moveForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImage();
                new Uploader().sendImage(path,pref.getString("ip",null));
                if(name.isChecked() && !signature.isChecked())
                    dialog(0);

                else if(signature.isChecked() && !name.isChecked()){
                    save(null);
                    startActivity(new Intent(Preview.this,Signature.class));
                }

                else if(name.isChecked() && signature.isChecked())
                    dialog(1);

                else {
                    save(null);
                    startActivity(new Intent(Preview.this, Shot.class));
                }
            }
        });
        retake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delImage(path);
                startActivity(new Intent(Preview.this,CameraActivity.class));
                finish();
            }
        });
        addImage();
    }



    private void dialog(final int j) {
        inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        layout = inflater.inflate(R.layout.namedialog,
                (ViewGroup) findViewById(R.id.nameLayout));
        nameBuilder = new AlertDialog.Builder(this);
        nameBuilder.setView(layout);
        nameBuilder.setTitle("Enter full name");
        nameBuilder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.i(MainActivity.TAG,"OKAY Pressed");
                EditText name =  layout.findViewById(R.id.name);
                String fullName = name.getText().toString();
                if(fullName.length() != 0){
                    Log.i(MainActivity.TAG,"\nname = " + fullName);
                    SharedPreferences.Editor editor = pref.edit();
                    if(j == 0){
                        save(fullName);
                        startActivity(new Intent(Preview.this,Shot.class));
                    }

                    else if (j == 1){
                        save(fullName);
                        startActivity(new Intent(Preview.this,Signature.class));
                    }
                    else if (j == 2){
                        editor.putString("ip",fullName);
                        editor.commit();
                    }

                }

            }
        });
        nameBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                nameDialog.dismiss();
            }
        });
        nameDialog = nameBuilder.create();
        nameDialog.show();

    }

    private void save(String name){
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("name",name);
        editor.putString("image",path);
        editor.commit();
    }

    private void addImage() {
        Log.i(MainActivity.TAG, "This is where the image came from -> " + path);
        final File imgFile = new File(path);
        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
        if(imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            Bitmap frame;
            if (orientation == 0)
                frame = BitmapFactory.decodeResource(getResources(), portrait[applyFrame]);
            else
                frame = BitmapFactory.decodeResource(getResources(), land[applyFrame]);
            combinedImage = combineImages(frame, myBitmap);
            preview.setImageBitmap(combinedImage);
            Log.i(MainActivity.TAG, "Bitmap added");

        }
    }


    private String saveImage(){
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        combinedImage.compress(Bitmap.CompressFormat.JPEG,100,boas);
        byte[] byteArray = boas.toByteArray();
        File pictureFile = new File(path);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflaterMenu = getMenuInflater();
        inflaterMenu.inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.ip){
            dialog(2);
            return true;
        }
        return false;
    }
    private boolean delImage(String path){
        File file = new File(path);
        if(file.exists())
            return file.delete();
        return false;
    }

    @Override
    public void onBackPressed() {
        delImage(path);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{path}, new String[]{"image/jpeg"}, null);

    }

    @Override
    protected void onResume() {
        super.onResume();
        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{path}, new String[]{"image/jpeg"}, null);

    }
    public Bitmap combineImages(Bitmap frame, Bitmap image) {

        Bitmap cs = null;
        Bitmap rs = null;
        Log.w(MainActivity.TAG, image.getWidth() + " X " + image.getHeight());
        rs = Bitmap.createScaledBitmap(frame, image.getWidth(),
                image.getHeight(), true);

        //Bitmap smallImage = Bitmap.createScaledBitmap(image,image.getWidth(),image.getHeight(),true);
        cs = Bitmap.createBitmap(image.getWidth(), image.getHeight(),
                Bitmap.Config.RGB_565);

        Canvas comboImage = new Canvas(cs);

        comboImage.drawBitmap(image, 0, 0, null);
        comboImage.drawBitmap(rs, 0, 0, null);


        if (rs != null) {
            rs.recycle();
            rs = null;
        }
        Runtime.getRuntime().gc();
        Log.w(MainActivity.TAG, cs.getWidth() + " X " + cs.getHeight());
        return cs;
    }
}
