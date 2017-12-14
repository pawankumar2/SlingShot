package com.example.slingshot;

/**
 * Created by lenovo-05 on 14-11-2017.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;


import java.io.File;
import java.io.IOException;

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

/**
 * Created by LENOVO on 7/17/2017.
 */

public class Uploader2  {
    private File dirWaiting;
    private File dirFailed;
    private File dirSuccess;
    Context mContext;
    private  String deviceName = null;

    Uploader2(Context context,String device){

        mContext = context;
        deviceName = device;
        new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"picshot").mkdir();
        dirWaiting = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "picshot/Waiting/");
        dirSuccess = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "picshot/Success/");
        dirFailed = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "picshot/Failed/");
        dirWaiting.mkdir();
        dirSuccess.mkdir();
        dirFailed.mkdir();


        if(new Check(mContext).internetConnection()){
            try{
                if (dirWaiting.listFiles().length != 0) {
                    File[] list = dirWaiting.listFiles();
                    upload(list[0]);
                }
            }catch (NullPointerException e){
                Log.e(Welcome.TAG,e.getMessage());
            }
        }

    }

    private void upload(final File file){
        String URL = "http://socialact.in";
        final String uid = getUid(file.getName());
        final String social = getSocial(file.getName());
        final String email = getEmail(file.getName());
        final String timestamp = getTimestamp(file.getName());
        Log.i(Welcome.TAG, uid + " " + social + " " + email);

        if(deviceName == null)
            Toast.makeText(mContext,"Add device name",Toast.LENGTH_LONG).show();
        else{
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

// Change base URL to your upload server URL.
            Service service = new Retrofit.Builder().baseUrl(URL).client(client).build().create(Service.class);


            RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("picture",file.getName(), reqFile);
            RequestBody s = RequestBody.create(MediaType.parse("text/plain"), social);
            RequestBody e = RequestBody.create(MediaType.parse("text/plain"), email);
            RequestBody d;
            d = RequestBody.create(MediaType.parse("text/plain"), deviceName);
            RequestBody t = RequestBody.create(MediaType.parse("text/plain"), uid);

            retrofit2.Call<okhttp3.ResponseBody> req = service.postImage(body, s,e,d,t);
            Log.w(Welcome.TAG,"uploading image...");
            req.enqueue(new Callback<ResponseBody>() {

                @Override
                public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                    //Log.i(MainActivity.TAG,response.message());
                    String resultResponse = null;
                    try {
                        resultResponse = response.body().string();
                    } catch (IOException e1){
                        e1.printStackTrace();
                    }
                    if (resultResponse.equalsIgnoreCase("SUCCESS")) {
                        Log.i(Welcome.TAG, dirSuccess.getPath() + File.separator + file.getName());
                        moveFile(dirSuccess.getPath() + File.separator + file.getName(), file);
                        Log.i(Welcome.TAG, "Image uploaded...");
                        Toast.makeText(mContext, "Image Uploaded", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.i(Welcome.TAG, dirFailed.getPath() + File.separator + file.getName());
                        moveFile(dirFailed.getPath() + File.separator + file.getName(), file);
                        Toast.makeText(mContext, "Failed" + resultResponse, Toast.LENGTH_LONG).show();
                        Log.i(Welcome.TAG, "image upload failed..." + resultResponse);
                    }
                    if (dirWaiting.listFiles().length != 0) {
                        File[] list = dirWaiting.listFiles();
                        Log.w(Welcome.TAG,"next image");
                        upload(list[0]);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(Welcome.TAG,t.getMessage());
                    Toast.makeText(mContext, "Failed", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    private String getTimestamp(String name){
        return name.split("__")[3];
    }

    private String getEmail(String name) {
        return name.split("__")[2];
    }

    private String getSocial(String name) {
        return name.split("__")[1];
    }

    private String getUid(String name) {
        return name.split("__")[0];
    }
    private void moveFile(String dir,File file){
        File to = new File(dir);
        file.renameTo(to);
        delImage(file);
        MediaScannerConnection.scanFile(mContext, new String[] { to.getPath() }, new String[] { "image/jpeg" }, null);
        MediaScannerConnection.scanFile(mContext, new String[] { file.getPath() }, new String[] { "image/jpeg" }, null);

    }
    public boolean delImage(File file){
        if(file.exists()){
            return file.delete();
        }
        return false;
    }
    interface Service {
        @Multipart
        @POST("/api/post/picture/")
        Call<ResponseBody> postImage(@Part MultipartBody.Part image
                , @Part("social") RequestBody social
                , @Part("email") RequestBody email
                , @Part("device") RequestBody device
                , @Part("tagid") RequestBody tagid);
    }

}

