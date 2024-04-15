package com.example.new_down;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DatabaseHandler extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "DownloadManager";
    public static final int DATABASE_VERSION = 2;
    public static final String TABLE_NAME = "Downloads";
    public static final String ID = "id";
    public static final String DOWNLOAD_ID = "downloadId";
    public static final String TITLE = "title";
    public static final String FILE_PATH = "file_path";
    public static final String PROGRESS = "progress";
    public static final String STATUS = "status";
    public static final String FILE_SIZE = "file_size";
    public static final String IS_PAUSED = "is_paused";
    public static final String URL = "url";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String create_students_table = String.format("Create table %s ( %s INTEGER  PRIMARY KEY AUTOINCREMENT, %s INTEGER, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s TEXT, %s INTEGER, %s  TEXT )", TABLE_NAME, ID, DOWNLOAD_ID, TITLE, FILE_PATH, PROGRESS, STATUS, FILE_SIZE, IS_PAUSED, URL);
        db.execSQL(create_students_table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = String.format("DROP TABLE IF EXISTS %s", TABLE_NAME);
        db.execSQL(sql);
        onCreate(db);
    }

    public void addDown(DownloadModel model) {

        SQLiteDatabase db = this.getWritableDatabase();
        Random random = new Random();

        ContentValues values = new ContentValues();
        values.put(DOWNLOAD_ID, model.getDownloadId());
        values.put(TITLE, model.getTitle());
        values.put(FILE_PATH, model.getFile_path());
        values.put(PROGRESS, model.getProgress());
        values.put(STATUS, model.getStatus());
        values.put(FILE_SIZE, model.getFile_size());
        values.put(IS_PAUSED, model.isIs_paused() ? 1 : 0);
        values.put(URL, model.getUrl());

        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public DownloadModel getByDownID(long ID) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, null, DOWNLOAD_ID + " = ?", new String[] { String.valueOf(ID) },null, null, null);
        if(cursor != null)
            cursor.moveToFirst();
        DownloadModel down = new DownloadModel();
        down.setId(cursor.getInt(0));
        down.setDownloadId(cursor.getInt(1));
        down.setFile_path(cursor.getString(3));
        down.setFile_size(cursor.getString(6));
        down.setProgress(cursor.getString(4));
        down.setStatus(cursor.getString(5));
        down.setTitle(cursor.getString(2));
        boolean flag = false;
        if (cursor.getInt(7) >= 1)
            flag = true;
        down.setIs_paused(flag);
        down.setUrl(cursor.getString(8));
        return down;
    }


    public List<DownloadModel> getAllDown() {
        List<DownloadModel> datalist = new ArrayList<>();
        String query = " SELECT * FROM " + TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            DownloadModel down = new DownloadModel();
            down.setId(cursor.getInt(0));
            down.setDownloadId(cursor.getInt(1));
            down.setFile_path(cursor.getString(3));
            down.setFile_size(cursor.getString(6));
            down.setProgress(cursor.getString(4));
            down.setStatus(cursor.getString(5));
            down.setTitle(cursor.getString(2));
            boolean flag = false;
            if (cursor.getInt(7) >= 1)
                flag = true;
            down.setIs_paused(flag);
            down.setUrl(cursor.getString(8));
            datalist.add(down);
            cursor.moveToNext();
        }
        cursor.close();
        return datalist;
    }

    public void deleteDown(DownloadModel model) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, ID + " = ?", new String[]{Long.toString(model.getId())});
        db.close();
    }

    public void clear() {
        List<DownloadModel> data = getAllDown();
        for (DownloadModel i : data) {
            deleteDown(i);
        }
    }

    public void update(DownloadModel model) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TITLE, model.getTitle());
        values.put(FILE_PATH, model.getFile_path());
        values.put(PROGRESS, model.getProgress());
        values.put(STATUS, model.getStatus());
        values.put(FILE_SIZE, model.getFile_size());
        values.put(IS_PAUSED, model.isIs_paused() ? 1 : 0);
        values.put(URL, model.getUrl());

        db.update(TABLE_NAME, values, ID + " = ?", new String[]{String.valueOf(model.getId())});
        db.close();
    }
}