package com.example.hackves;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.hackves.databinding.LoadingBinding;

public class Helper {

    public static final String TYPE_AUDIO = MediaStore.Audio.Media.DATA;
    public static final String TYPE_VIDEO = MediaStore.Video.Media.DATA;

    public static final int ADD_AUDIO = 0;
    public static final int FADE_IN = 1;
    public static final int FADE_OUT = 2;

    public static String getFilePath(Context context, Uri uri, String contentType){
        String[] columns = {contentType};
        String filePath;
        Cursor c = context.getContentResolver().query(uri, columns, null, null, null);
        if(c.moveToFirst()){
            int index = c.getColumnIndex(columns[0]);
            filePath = c.getString(index);
        } else {
            Toast.makeText(context , "File not found" , Toast.LENGTH_SHORT).show();
            return "";
        }
        c.close();

        return filePath;
    }

    public static AlertDialog buildLoadingDialog(Context context){
        LoadingBinding binding = LoadingBinding.inflate(LayoutInflater.from(context));
        return new AlertDialog.Builder(context)
                .setView(binding.getRoot())
                .setCancelable(false)
                .create();
    }
}
