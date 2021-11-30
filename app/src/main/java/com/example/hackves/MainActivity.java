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
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.MediaController;
import android.widget.Toast;

import com.example.hackves.databinding.ActivityMainBinding;
import com.example.hackves.databinding.AddAudioDialogBinding;
import com.example.hackves.databinding.ExportVideoDialogBinding;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_PATH = "path";

    ActivityMainBinding binding;
    ExportVideoDialogBinding exportVideoDialogBinding;
    FFmpeg fFmpeg;
    AlertDialog messageDialog, exportOptionsDialog;
    BottomSheetDialog addMusicDialog;
    AddAudioDialogBinding addMusicBinding;
    ActivityResultLauncher<String> getAudioLauncher;
    String videoPath, audioPath;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Register activity result launcher for audio
        getAudioLauncher = registerForActivityResult(new ActivityResultContracts.GetContent() , new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result){
                audioPath = Helper.getFilePath(MainActivity.this, result, Helper.TYPE_AUDIO);
                String fileName = new File(audioPath).getName();
                addMusicBinding.fileName.setText(fileName);
            }
        });

        MediaController controller = new MediaController(this);
        controller.setAnchorView(binding.videoAnchor);
        videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        Uri uri = Uri.parse(videoPath);
        binding.video.setVideoURI(uri);
        binding.video.setMediaController(controller);
        binding.video.start();
//        binding.pickAudio.setOnClickListener(view -> getAudioLauncher.launch("*/*"));

        buildDialogs();
        initFFmpeg();

        binding.audio.setOnClickListener(view -> addMusicDialog.show());
    }

    private void buildDialogs( ){
        messageDialog = new AlertDialog.Builder(this)
                .setTitle("Error")
                .setPositiveButton("Ok", null)
                .create();

        exportVideoDialogBinding = ExportVideoDialogBinding.inflate(getLayoutInflater());
        exportOptionsDialog = new AlertDialog.Builder(this)
                .setTitle("Export")
                .setView(exportVideoDialogBinding.getRoot())
                .create();

        addMusicDialog = new BottomSheetDialog(this);
        addMusicBinding = AddAudioDialogBinding.inflate(getLayoutInflater());
        addMusicDialog.setContentView(addMusicBinding.getRoot());
        addMusicBinding.add.setOnClickListener(view -> {
            addMusicDialog.dismiss();
            addAudio();
        });
        addMusicBinding.cancel.setOnClickListener(view -> addMusicDialog.dismiss());
        addMusicBinding.choose.setOnClickListener(view -> getAudioLauncher.launch("*/*"));
    }

    private void showUnsupportedError(){
        messageDialog.setMessage("App not supported on your device");
        messageDialog.setButton(AlertDialog.BUTTON_POSITIVE , "Ok" , (dialogInterface , i) -> {
            System.exit(0);
        });
    }

    private void initFFmpeg(){
        if(fFmpeg == null){
            fFmpeg = FFmpeg.getInstance(this);
            try {
                fFmpeg.loadBinary(new LoadBinaryResponseHandler() {
                    @Override
                    public void onFailure( ){
                        showUnsupportedError();
                    }
                });
            } catch (FFmpegNotSupportedException e) {
                e.printStackTrace();
            }
        }
    }

    private void executeFFmpegCommand(String []cmd, CommandResultListener listener){
        try {
            fFmpeg.execute(cmd, new ExecuteBinaryResponseHandler(){
                @Override
                public void onSuccess(String message){
                    super.onSuccess(message);
                    if(listener!=null){
                        listener.onSuccess(message);
                    }
                }

                @Override
                public void onFailure(String message){
                    super.onFailure(message);
                    if(listener!=null){
                        listener.onFailure(message);
                    }
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    private void addAudio(){
        exportOptionsDialog.setButton(DialogInterface.BUTTON_POSITIVE , "Save" , (dialogInterface , i) -> {
            File dir = new File(Environment.getExternalStorageDirectory(),"HackVES");
            if(!dir.exists()){
                dir.mkdir();
            }

            File outputFile = new File(dir, exportVideoDialogBinding.fileName.getText()+""+exportVideoDialogBinding.fileFormat.getSelectedItem());

            String[] command  = {"-i", videoPath, "-i", audioPath, "-c:v", "copy", "-c:a", "aac", "-map", "0:v:0", "-map", "1:a:0", "-shortest", outputFile.getAbsolutePath()};
            executeFFmpegCommand(command , new CommandResultListener() {
                @Override
                public void onSuccess(String message){
                    binding.video.setVideoURI(Uri.fromFile(outputFile));
                    binding.video.start();
                    Toast.makeText(getApplicationContext() , "Success" , Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String message){
                    Toast.makeText(getApplicationContext() , "Failure" , Toast.LENGTH_SHORT).show();
                }
            });
        });
        exportOptionsDialog.show();
    }
}