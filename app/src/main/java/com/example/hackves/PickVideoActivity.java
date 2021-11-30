package com.example.hackves;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.Toast;

import com.example.hackves.databinding.ActivityPickVideoBinding;

public class PickVideoActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;
    ActivityPickVideoBinding binding;
    private ActivityResultLauncher<String> getVideoLauncher;
    private String videoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        binding = ActivityPickVideoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Register activity result launcher for video
        getVideoLauncher = registerForActivityResult(new ActivityResultContracts.GetContent() , new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result){
                videoPath = Helper.getFilePath(PickVideoActivity.this, result, Helper.TYPE_VIDEO);
                Intent intent = new Intent(PickVideoActivity.this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_VIDEO_PATH, videoPath);
                startActivity(intent);
            }
        });

        binding.chooseButton.setOnClickListener(view -> pickVideo());
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
            getVideoLauncher.launch("*/*");
        }else{
            Toast.makeText(PickVideoActivity.this , "Permission not granted" , Toast.LENGTH_SHORT).show();
        }
    }
}