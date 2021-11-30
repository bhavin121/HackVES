package com.example.hackves;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.MediaController;
import android.widget.Toast;

import com.example.hackves.databinding.ActivityMainBinding;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    public static final String TYPE_AUDIO = MediaStore.Audio.Media.DATA;
    public static final String TYPE_VIDEO = MediaStore.Video.Media.DATA;
    public static final int REQUEST_CODE = 100;

    ActivityMainBinding binding;
    FFmpeg fFmpeg;
    AlertDialog messageDialog;
    ActivityResultLauncher<String> getVideoLauncher, getAudioLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Register activity result launcher for video
        getVideoLauncher = registerForActivityResult(new ActivityResultContracts.GetContent() , new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result){
                System.out.println(getFilePath(result, TYPE_VIDEO));
                binding.video.setVideoURI(result);
                binding.video.setMediaController(new MediaController(MainActivity.this));
                binding.video.start();
            }
        });

        // Register activity result launcher for audio
        getAudioLauncher = registerForActivityResult(new ActivityResultContracts.GetContent() , new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result){

            }
        });

        binding.pickVideo.setOnClickListener(view -> pickVideo());

        buildMessageDialog();
        initFFmpeg();
    }

    private void pickVideo(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==PackageManager.PERMISSION_GRANTED){
            getVideoLauncher.launch("*/*");
        }
        else{
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    , REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode , @NonNull String[] permissions , @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode , permissions , grantResults);

        if(requestCode == REQUEST_CODE &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED
        ){
            getVideoLauncher.launch("video/*");
        }else{
            Toast.makeText(MainActivity.this , "Permission not granted" , Toast.LENGTH_SHORT).show();
        }
    }

    private void buildMessageDialog( ){
        messageDialog = new AlertDialog.Builder(this)
                .setTitle("Error")
                .setPositiveButton("Ok", null)
                .setNegativeButton("Cancel", null)
                .create();
    }

    private void initFFmpeg(){
        if(fFmpeg == null){
            fFmpeg = FFmpeg.getInstance(this);
            try {
                fFmpeg.loadBinary(new FFmpegLoadBinaryResponseHandler() {
                    @Override
                    public void onFailure( ){
                        showUnsupportedError();
                    }

                    @Override
                    public void onSuccess( ){
                        Toast.makeText(getApplicationContext() , "success" , Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onStart( ){

                    }

                    @Override
                    public void onFinish( ){

                    }
                });
            } catch (FFmpegNotSupportedException e) {
                e.printStackTrace();
            }
        }
    }

    private void executeFFmpegCommand(String []cmd){
        try {
            fFmpeg.execute(cmd, new ExecuteBinaryResponseHandler(){
                @Override
                public void onSuccess(String message){
                    super.onSuccess(message);
                }

                @Override
                public void onFailure(String message){
                    super.onFailure(message);
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    private void showUnsupportedError(){
        messageDialog.setMessage("App not supported on your device");
        messageDialog.setButton(AlertDialog.BUTTON_POSITIVE , "Ok" , (dialogInterface , i) -> {
            System.exit(0);
        });
    }

    public String getFilePath(Uri uri, String contentType){
        String[] columns = {contentType};
        String filePath;
        Cursor c = getContentResolver().query(uri, columns, null, null, null);
        if(c.moveToFirst()){
            int index = c.getColumnIndex(columns[0]);
            filePath = c.getString(index);
        } else {
            Toast.makeText(MainActivity.this , "File not found" , Toast.LENGTH_SHORT).show();
            return "";
        }
        c.close();

        return filePath;
    }

}