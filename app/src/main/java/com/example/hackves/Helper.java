package com.example.hackves;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;

public class Helper {

    public static final String TYPE_AUDIO = MediaStore.Audio.Media.DATA;
    public static final String TYPE_VIDEO = MediaStore.Video.Media.DATA;

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
}
