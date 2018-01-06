package com.example.picshot;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.print.PrintHelper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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

public class Preview extends AppCompatActivity {

    private String path;
    private ImageView preview;
    private Button moveForward;
    private LayoutInflater inflater;
    private View layout;
    private AlertDialog.Builder nameBuilder;
    private AlertDialog nameDialog;
    private SharedPreferences pref;
    private Button retake;
    private int orientation;
    private File [] portrait;// = {R.drawable.overlay,R.drawable.photoframe};
    private int [] land = {R.drawable.overlay0,R.drawable.photoframe0};
    private  Bitmap combinedImage;
    private int applyFrame;
    private  File dirWaiting;
    private String newPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        path = getIntent().getStringExtra("path");
        orientation = getIntent().getIntExtra("orientation",0);
        applyFrame = getIntent().getIntExtra("frame",0);
        ContextWrapper cWrapper = new ContextWrapper(this);
        portrait = new File(cWrapper.getFilesDir().getAbsolutePath() + "/Frames/portrait0").listFiles();
        preview = (ImageView) findViewById(R.id.preview);
        final CheckBox fb = (CheckBox) findViewById(R.id.share);
        moveForward = (Button) findViewById(R.id.moveForward);
        retake = (Button) findViewById(R.id.retake);
        pref = getApplicationContext().getSharedPreferences("data", Context.MODE_PRIVATE);
        final CheckBox print = (CheckBox) findViewById(R.id.print);
        dirWaiting = new File( Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "picshot/Waiting/");
        dirWaiting.mkdir();
        final CheckBox email = (CheckBox)findViewById(R.id.email);
        moveForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               String ip = pref.getString("ip",null);
                int i = 0;
                int j =0;

                if(fb.isChecked() || email.isChecked() || print.isChecked()){
                    if(fb.isChecked())
                        i = 1;
                    if(email.isChecked())
                        j=1;
                    if (print.isChecked()){
                        Toast.makeText(getApplicationContext(),"printing...", Toast.LENGTH_LONG).show();
                        print();
                    }
                    saveImage(i,j,ip);
                  //  if(pref.getString("name",null) == ""){
                    //    dialog();
                    //}
                   // else{
                        startActivity(new Intent(Preview.this,Shot.class));
                        finish();

                   // }

                }
                else
                    Toast.makeText(getApplicationContext(),"Please select an option from above",Toast.LENGTH_LONG).show();
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



    private void dialog() {
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
                EditText name =  layout.findViewById(R.id.nameDialog);
                String fullName = name.getText().toString();
                if(fullName.length() != 0){
                    Log.i(MainActivity.TAG,"\nname = " + fullName);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("name",fullName);
                    editor.putString("image",newPath);
                    editor.commit();
                        startActivity(new Intent(Preview.this,Shot.class));
                        finish();

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


    private void addImage() {
        Log.i(MainActivity.TAG, "This is where the image came from -> " + path);
        final File imgFile = new File(path);

        if(imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            Bitmap frame;
            if(portrait.length != 0){
                if (orientation == 0)
                    frame =  bitmap(portrait[applyFrame]);
                else
                    frame = BitmapFactory.decodeResource(getResources(), land[applyFrame]);
                combinedImage = combineImages(frame, myBitmap);
            }
            else
                combinedImage = myBitmap;
            preview.setImageBitmap(combinedImage);
            Log.i(MainActivity.TAG, "Bitmap added");

        }
    }
    private Bitmap bitmap(File file){
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    private void saveImage(final int i, final int j , final String ip){

        final Bitmap image = combinedImage;
        final File pictureFile = getOutputMediaFile(i,j);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("image",pictureFile.getAbsolutePath());
        editor.commit();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(byteArray);
                    Log.i(Welcome.TAG,"file saved");
                    fos.close();
                    Log.i(Welcome.TAG,"stream closed");
                } catch (FileNotFoundException e) {
                    Log.e(Welcome.TAG,e.getMessage());

                } catch (IOException e) {
                    Log.e(Welcome.TAG,e.getMessage());
                }
                delImage(path);
                MediaScannerConnection.scanFile(getApplicationContext(), new String[] { path }, new String[] { "image/jpeg" }, null);
                MediaScannerConnection.scanFile(getApplicationContext(), new String[] { pictureFile.getPath() }, new String[] { "image/jpeg" }, null);
                newPath = pictureFile.getPath();

                new Uploader().sendImage(newPath,ip);
                return null;
            }


        }.execute();


    }



    private  File getOutputMediaFile(int i, int j) {

        Long timeStamp = System.currentTimeMillis();
        Log.i(Welcome.TAG, String.valueOf(timeStamp));
        String uid = pref.getString("band_uid","null");
        Log.i(Welcome.TAG,uid);
        File mediaFile;
        mediaFile = new File(dirWaiting.getPath() + File.separator + uid
                + "___"+ i + "___" + j+ "___"+ timeStamp + ".jpeg");
        Log.i(Welcome.TAG,"Got file");
        Log.i(Welcome.TAG,mediaFile.getParent() + " - " + mediaFile.getName() + " - " + mediaFile.getPath());

        return mediaFile;
    }


    public void print(){
        PrintHelper photoPrinter = new PrintHelper(Preview.this );
        photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        photoPrinter.printBitmap("image", combinedImage);
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
