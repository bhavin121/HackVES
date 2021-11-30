package com.example.hackves;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.MediaController;
import android.widget.Toast;

import com.example.hackves.databinding.ActivityMainBinding;
import com.example.hackves.databinding.AddAudioDialogBinding;
import com.example.hackves.databinding.EffectDialogBinding;
import com.example.hackves.databinding.ExportVideoDialogBinding;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_PATH = "path";

    public static final int ADD_AUDIO = 0;
    public static final int FADE_IN = 1;
    public static final int FADE_OUT = 2;

    ActivityMainBinding binding;
    ExportVideoDialogBinding exportVideoDialogBinding;
    FFmpeg fFmpeg;
    AlertDialog messageDialog, exportOptionsDialog;
    BottomSheetDialog addMusicDialog, addEffectDialog;
    AddAudioDialogBinding addMusicBinding;
    EffectDialogBinding effectDialogBinding;
    ActivityResultLauncher<String> getAudioLauncher;
    String videoPath, audioPath;
    MediaPlayer videoPlayer;

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
        videoPlayer = new MediaPlayer();
        try {
            videoPlayer.setDataSource(uri.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        binding.pickAudio.setOnClickListener(view -> getAudioLauncher.launch("*/*"));

        buildDialogs();
        initFFmpeg();

        binding.audio.setOnClickListener(view -> addMusicDialog.show());
        binding.effect.setOnClickListener(view -> addEffectDialog.show());
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
            applyEffect(ADD_AUDIO);
        });
        addMusicBinding.cancel.setOnClickListener(view -> addMusicDialog.dismiss());
        addMusicBinding.choose.setOnClickListener(view -> getAudioLauncher.launch("*/*"));

        effectDialogBinding = EffectDialogBinding.inflate(getLayoutInflater());
        addEffectDialog = new BottomSheetDialog(this);
        addEffectDialog.setContentView(effectDialogBinding.getRoot());
        effectDialogBinding.fadeIn.setOnClickListener(view -> {
            applyEffect(FADE_IN);
            addEffectDialog.dismiss();
        });
        effectDialogBinding.fadeOut.setOnClickListener(view ->{
            applyEffect(FADE_OUT);
            addEffectDialog.dismiss();
        });
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

    private void applyEffect(int effect){
        exportOptionsDialog.setButton(DialogInterface.BUTTON_POSITIVE , "Save" , (dialogInterface , i) -> {
            File dir = new File(Environment.getExternalStorageDirectory(),"HackVES");
            if(!dir.exists()){
                dir.mkdir();
            }

            File outputFile = new File(dir, exportVideoDialogBinding.fileName.getText()+""+exportVideoDialogBinding.fileFormat.getSelectedItem());


            executeFFmpegCommand(getCommands(effect, outputFile) , new CommandResultListener() {
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

    public String[] getCommands(int effect, File dest){
        String[] command = null;
        switch (effect){
            case ADD_AUDIO:
                command  = new String[]{"-i", videoPath, "-i", audioPath, "-c:v", "copy", "-c:a", "aac", "-map", "0:v:0", "-map", "1:a:0", "-shortest", dest.getAbsolutePath()};
                break;
            case FADE_IN:
                command = new String[]{"-y", "-i", videoPath, "-acodec", "copy", "-vf", "fade=t=in:st=0:d=5", dest.getAbsolutePath()};
                break;
            case FADE_OUT:
                command = new String[]{"-y", "-i", videoPath, "-acodec", "copy", "-vf", "fade=t=out:st=" + String.valueOf(binding.video.getDuration() - 5) + ":d=5", dest.getAbsolutePath()};
                break;
        }
        return command;
    }
}