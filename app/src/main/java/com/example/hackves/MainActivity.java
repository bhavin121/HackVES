package com.example.hackves;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.MediaController;
import android.widget.Toast;

import com.example.hackves.databinding.ActivityMainBinding;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

public class MainActivity extends AppCompatActivity {

    public static final String TYPE_AUDIO = MediaStore.Audio.Media.DATA;
    public static final String TYPE_VIDEO = MediaStore.Video.Media.DATA;

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
                System.out.println(result);
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

        binding.pickVideo.setOnClickListener(view -> {
            pickVideo();
        });
        buildMessageDialog();
        initFFmpeg();
    }

    private void pickVideo(){
        getVideoLauncher.launch("video/*");
    }

    private void buildMessageDialog( ){
        messageDialog = new AlertDialog.Builder(this)
                .setTitle("Error")
                .setPositiveButton("Ok", null)
                .setNegativeButton("Cancel", null)
                .create();
    }

    private void initFFmpeg( ){
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